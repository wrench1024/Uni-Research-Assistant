import os
import json
import uvicorn
import requests
from fastapi import FastAPI, HTTPException, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
from fastapi.responses import StreamingResponse
from starlette.concurrency import run_in_threadpool
import tempfile
import shutil

from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Import RAG service
try:
    from rag_service import ingest_document, search_context, build_rag_prompt, delete_document_vectors, get_document_chunks
    RAG_ENABLED = True
    print("RAG service loaded successfully")
except ImportError as e:
    RAG_ENABLED = False
    print(f"RAG service not available: {e}")

app = FastAPI(title="LLM Research Assistant AI Service")

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configuration - Official DeepSeek API
API_KEY = os.getenv("DEEPSEEK_API_KEY")
BASE_URL = "https://api.deepseek.com/v1/chat/completions"
MODEL_NAME = "deepseek-chat"

# Models
class ChatMessage(BaseModel):
    role: str
    content: str

class ChatRequest(BaseModel):
    message: str
    history: Optional[List[ChatMessage]] = []
    model: str = MODEL_NAME
    use_rag: bool = True  # Enable RAG by default
    doc_id: Optional[str] = None  # Optional: limit search to specific document

class IngestRequest(BaseModel):
    file_path: str
    doc_id: Optional[str] = None

class AnalysisRequest(BaseModel):
    doc_id: str
    type: str = "summary"  # summary, key_points

class ComparisonRequest(BaseModel):
    doc_ids: List[str]
    aspects: Optional[List[str]] = None

class WriteRequest(BaseModel):
    text: str
    instruction: str  # polish, expand, continue, fix_grammar
    context: Optional[str] = None

@app.get("/")
def read_root():
    return {
        "status": "ok", 
        "service": "LLM Research Assistant AI Service (DeepSeek)",
        "rag_enabled": RAG_ENABLED
    }

@app.post("/api/v1/ingest")
async def ingest_endpoint(file: UploadFile = File(...), doc_id: Optional[str] = None):
    """
    Ingest a document into the vector database.
    Accepts file upload and processes it for RAG.
    """
    if not RAG_ENABLED:
        raise HTTPException(status_code=503, detail="RAG service is not available")
    
    print(f"=== Ingesting document: {file.filename} ===")
    
    # Save uploaded file to temp location
    try:
        suffix = os.path.splitext(file.filename)[1]
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
            shutil.copyfileobj(file.file, tmp)
            tmp_path = tmp.name
        
        # Process document
        result = await run_in_threadpool(ingest_document, tmp_path, doc_id or file.filename)
        
        # Cleanup temp file
        os.unlink(tmp_path)
        
        return result
    except Exception as e:
        print(f"Ingestion error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/v1/ingest/path")
async def ingest_by_path(request: IngestRequest):
    """
    Ingest a document by file path (for internal use by Java backend).
    """
    if not RAG_ENABLED:
        raise HTTPException(status_code=503, detail="RAG service is not available")
    
    print(f"=== Ingesting document by path: {request.file_path} ===")
    
    if not os.path.exists(request.file_path):
        raise HTTPException(status_code=404, detail=f"File not found: {request.file_path}")
    
    try:
        result = await run_in_threadpool(ingest_document, request.file_path, request.doc_id)
        return result
    except Exception as e:
        print(f"Ingestion error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

class DeleteRequest(BaseModel):
    doc_id: str

@app.delete("/api/v1/vectors/{doc_id}")
async def delete_vectors(doc_id: str):
    """
    Delete all vectors associated with a document.
    Called when a document is deleted from the system.
    """
    if not RAG_ENABLED:
        raise HTTPException(status_code=503, detail="RAG service is not available")
    
    print(f"=== Deleting vectors for doc_id: {doc_id} ===")
    
    try:
        result = await run_in_threadpool(delete_document_vectors, doc_id)
        return result
    except Exception as e:
        print(f"Delete error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/v1/chat/stream")
async def stream_chat(request: ChatRequest):
    print(f"=== Received chat request ===")
    print(f"Message: {request.message}")
    print(f"History count: {len(request.history)}")
    print(f"RAG enabled: {request.use_rag and RAG_ENABLED}")

    # Build messages with history
    messages = []
    
    # RAG: Search for relevant context
    system_prompt = None
    citations = []
    
    if request.use_rag and RAG_ENABLED:
        try:
            # search_context now returns list of dicts with metadata
            context_results = await run_in_threadpool(
                search_context, 
                request.message, 
                k=4, 
                doc_id=request.doc_id
            )
            
            if context_results:
                # Extract text for prompt
                context_chunks = [item["text"] for item in context_results]
                
                # Prepare citations for frontend
                citations = []
                for item in context_results:
                    citations.append({
                        "doc_id": item.get("doc_id"),
                        "text": item.get("text")[:200] + "...", # Preview
                        "chunk_index": item.get("chunk_index"),
                        "source_file": item.get("source_file")
                    })
                
                system_prompt = build_rag_prompt(request.message, context_chunks)
                print(f"RAG: Found {len(context_results)} context chunks")
                
                # Emit citation event properly as JSON
                citation_event = {
                    "type": "citation",
                    "citations": citations
                }
                # Double newline is handled by stream_llm_response wrapper usually, 
                # but here we yield it directly before the generator starts
                # We need to make sure stream_llm_response handles this or yield it here
                
        except Exception as e:
            print(f"RAG search failed: {e}")
            import traceback
            traceback.print_exc()
            # Continue without RAG context
    
    # Add system prompt if RAG found context
    if system_prompt:
        messages.append({"role": "system", "content": system_prompt})
    
    # Add conversation history
    for msg in request.history:
        messages.append({"role": msg.role, "content": msg.content})
    messages.append({"role": "user", "content": request.message})

    return StreamingResponse(stream_llm_response(messages, initial_event=citation_event if citations else None), media_type="text/event-stream")

@app.post("/api/v1/analyze/summary")
async def analyze_summary(request: AnalysisRequest):
    """
    Generate a summary for a specific document.
    Uses Map-Reduce strategy for large documents.
    """
    if not RAG_ENABLED:
        raise HTTPException(status_code=503, detail="RAG service is not available")
    
    print(f"=== Generating summary for: {request.doc_id} ===")
    
    # 1. Get all text chunks
    chunks = await run_in_threadpool(get_document_chunks, request.doc_id)
    if not chunks:
        raise HTTPException(status_code=404, detail=f"No content found for document: {request.doc_id}")
    
    full_text = "\n\n".join(chunks)
    total_chars = len(full_text)
    
    print(f"Document size: {total_chars} characters, {len(chunks)} chunks")
    
    # 2. Choose strategy based on document size
    if total_chars <= 30000:
        # Small document: Direct summarization
        print("Using direct summarization (small document)")
        return StreamingResponse(
            direct_summarize(full_text), 
            media_type="text/event-stream"
        )
    else:
        # Large document: Map-Reduce summarization
        print(f"Using Map-Reduce summarization (large document: {total_chars} chars)")
        return StreamingResponse(
            map_reduce_summarize(chunks), 
            media_type="text/event-stream"
        )


def direct_summarize(text: str):
    """Direct summarization for small documents."""
    system_prompt = """你是一个专业的学术研究助手。请仔细阅读用户提供的文档内容，并生成一份高质量的摘要。
摘要应包含：
1. 核心研究问题
2. 主要方法
3. 关键发现与结论
请使用 Markdown 格式，层级清晰。"""

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": f"请总结以下文档内容：\n\n{text}"}
    ]
    
    yield from stream_llm_response(messages)


def map_reduce_summarize(chunks: List[str]):
    """
    Map-Reduce summarization for large documents.
    
    Steps:
    1. Group chunks into sections (~20k chars each)
    2. Summarize each section (Map)
    3. Combine section summaries (Reduce)
    """
    # === Step 1: Group chunks into sections ===
    SECTION_SIZE = 20000  # 每个部分约 20k 字符
    sections = []
    current_section = []
    current_size = 0
    
    for chunk in chunks:
        chunk_size = len(chunk)
        if current_size + chunk_size > SECTION_SIZE and current_section:
            # Start new section
            sections.append("\n\n".join(current_section))
            current_section = [chunk]
            current_size = chunk_size
        else:
            current_section.append(chunk)
            current_size += chunk_size
    
    # Add last section
    if current_section:
        sections.append("\n\n".join(current_section))
    
    print(f"Split into {len(sections)} sections for Map-Reduce")
    
    # === Step 2: Map - Summarize each section ===
    yield "data: 📊 **开始分段处理文档** (共 {0} 个部分)...\n\n".format(len(sections))
    
    section_summaries = []
    for i, section in enumerate(sections):
        yield f"data: \n\n⏳ 正在处理第 {i+1}/{len(sections)} 部分...\n\n\n\n"
        
        # Generate section summary (non-streaming for internal processing)
        summary = generate_section_summary(section, i + 1)
        section_summaries.append(summary)
        
        yield f"data: ✅ 完成第 {i+1} 部分\n\n\n\n"
    
    # === Step 3: Reduce - Combine section summaries ===
    yield "data: \n\n🔄 **汇总所有部分**...\n\n\n\n"
    
    combined_summaries = "\n\n".join([
        f"【第 {i+1} 部分摘要】\n{summary}" 
        for i, summary in enumerate(section_summaries)
    ])
    
    # Final synthesis
    final_prompt = f"""你是一个学术研究专家。我已经将一份长文档分成了 {len(sections)} 个部分，并对每个部分生成了摘要。
现在请你将这些部分摘要整合成一份连贯、完整的最终摘要。

要求：
1. 保留所有关键信息
2. 去除冗余内容
3. 确保逻辑连贯
4. 使用 Markdown 格式

以下是各部分摘要：

{combined_summaries}

请生成最终摘要："""
    
    messages = [
        {"role": "user", "content": final_prompt}
    ]
    
    yield "data: \n\n---\n\n## 📝 最终摘要\n\n\n\n"
    yield from stream_llm_response(messages)


def generate_section_summary(section_text: str, section_num: int) -> str:
    """
    Generate a summary for a single section (synchronous).
    Returns the summary text.
    """
    system_prompt = f"""你是一个学术研究助手。这是一份长文档的第 {section_num} 部分。
请生成一份简洁的摘要，包含这部分的关键信息。
摘要应简洁但完整，约 200-300 字。"""
    
    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": section_text[:15000]}  # Limit to prevent overflow
    ]
    
    # Call API synchronously (non-streaming)
    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json",
    }
    data = {
        "model": MODEL_NAME,
        "messages": messages,
        "stream": False  # Non-streaming for internal use
    }
    
    try:
        response = requests.post(BASE_URL, headers=headers, json=data, timeout=60)
        if response.status_code == 200:
            result = response.json()
            return result["choices"][0]["message"]["content"]
        else:
            return f"[摘要生成失败: HTTP {response.status_code}]"
    except Exception as e:
        print(f"Section summary error: {e}")
        return f"[摘要生成出错: {str(e)}]"


@app.post("/api/v1/analyze/comparison")
async def analyze_comparison(request: ComparisonRequest):
    """
    Compare multiple documents with structured comparison table.
    """
    if not RAG_ENABLED:
        raise HTTPException(status_code=503, detail="RAG service is not available")
    
    print(f"=== Comparing {len(request.doc_ids)} documents ===")
    
    # 1. Retrieve document contents
    doc_contents = []
    doc_titles = []
    for doc_id in request.doc_ids:
        chunks = await run_in_threadpool(get_document_chunks, doc_id)
        if chunks:
            text = "\n\n".join(chunks)[:15000]  # Limit each doc
            doc_contents.append(text)
            doc_titles.append(str(doc_id))
    
    if len(doc_contents) < 2:
        raise HTTPException(status_code=400, detail="Need at least 2 documents for comparison")
    
    # 2. Generate structured comparison
    return StreamingResponse(
        generate_structured_comparison(doc_contents, doc_titles, request.aspects),
        media_type="text/event-stream"
    )


def generate_structured_comparison(doc_contents: List[str], doc_titles: List[str], custom_aspects: Optional[List[str]] = None):
    """
    Generate structured comparison with table data and detailed analysis.
    
    Steps:
    1. Generate comparison table (JSON)
    2. Yield table as SSE event
    3. Generate detailed analysis (streaming)
    """
    # === Step 1: Define comparison dimensions ===
    if custom_aspects and len(custom_aspects) > 0:
        dimensions = custom_aspects
    else:
        dimensions = ["研究问题/目标", "研究方法", "主要发现", "创新点", "局限性"]
    
    # === Step 2: Generate comparison table ===
    yield "data: 📊 正在生成对比表格...\n\n"
    
    # Build prompt for table generation
    doc_contents_combined = ""
    for i, (title, content) in enumerate(zip(doc_titles, doc_contents)):
        doc_contents_combined += f"\n\n【文档 {i+1}: {title}】\n{content}\n"
    
    table_prompt = f"""你是一个学术对比分析专家。请对以下 {len(doc_contents)} 篇文档进行结构化对比分析。

对比维度：{', '.join(dimensions)}

文档内容：
{doc_contents_combined}

请严格按照以下 JSON 格式输出对比表格数据，不要添加任何其他文字：

{{
  "dimensions": {dimensions},
  "comparison": [
    ["{dimensions[0]}的文档1内容", "{dimensions[0]}的文档2内容", ...],
    ["{dimensions[1]}的文档1内容", "{dimensions[1]}的文档2内容", ...],
    ...
  ]
}}

要求：
1. 每个维度的内容要简洁（50-100字）
2. 突出关键差异
3. 使用专业术语"""

    # Call LLM to generate table (non-streaming for parsing)
    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json",
    }
    data = {
        "model": MODEL_NAME,
        "messages": [{"role": "user", "content": table_prompt}],
        "stream": False
    }
    
    try:
        response = requests.post(BASE_URL, headers=headers, json=data, timeout=60)
        if response.status_code == 200:
            result = response.json()
            table_text = result["choices"][0]["message"]["content"]
            
            # Parse JSON from response
            import re
            json_match = re.search(r'\{[\s\S]*\}', table_text)
            if json_match:
                table_json = json_match.group(0)
                # Emit table event
                table_event = {
                    "type": "comparison_table",
                    "documents": [{"id": title, "title": f"文档{i+1}"} for i, title in enumerate(doc_titles)],
                    "table_data": table_json  # String JSON to be parsed by frontend
                }
                yield f"data: {json.dumps(table_event, ensure_ascii=False)}\n\n"
            else:
                yield "data: ⚠️ 表格生成失败，将直接显示详细分析\n\n"
        else:
            yield "data: ⚠️ 表格生成失败，将直接显示详细分析\n\n"
    except Exception as e:
        print(f"Table generation error: {e}")
        yield "data: ⚠️ 表格生成失败，将直接显示详细分析\n\n"
    
    # === Step 3: Generate detailed analysis ===
    yield "data: \n\n---\n\n## 📝 详细对比分析\n\n\n\n"
    
    analysis_prompt = f"""你是一个专业的学术情报分析师。基于以下文档，生成一份深度对比分析报告。

文档内容：
{doc_contents_combined}

请从以下角度进行详细对比分析：
1. 研究背景与动机的异同
2. 方法论的差异与优劣
3. 核心发现的互补性或冲突
4. 创新点的比较
5. 应用前景与局限性

**格式要求**：
- 使用 Markdown 格式
- 使用标题、列表、加粗等格式组织内容
- **禁止使用 Markdown 表格**（对比表格已在上方单独展示）
- 层次清晰，内容详实"""
    
    messages = [{"role": "user", "content": analysis_prompt}]
    yield from stream_llm_response(messages)


@app.post("/api/v1/write/process")
async def write_process(request: WriteRequest):
    """
    Process text for writing assistance (polish, expand, etc).
    """
    instruction_map = {
        "polish": "请润色以下文本，使其更加学术化、正式且流畅，修正语法错误，但保持原意：",
        "expand": "请对以下观点进行扩写，补充更多细节、论据或背景信息，使其内容更充实：",
        "continue": "请根据以下上文，续写一段逻辑连贯的内容：",
        "fix_grammar": "请检查并修正以下文本的语法和拼写错误，输出修正后的版本："
    }
    
    specific_instruction = instruction_map.get(request.instruction, "请处理以下文本：")
    
    system_prompt = """你是一个资深的学术写作导师。
    【重要规则】
    1. 你必须直接输出处理后的文本内容。
    2. 严禁包含任何解释、前言、后缀、由于、改写说明等元数据。
    3. 即使文本很短，也只输出结果。
    4. 【格式要求】：
       - **可以使用** 序号标题来组织结构（例如："1. 研究背景" 或 "一、 方法描述"），这将便于后续生成 Word 目录。
       - **不要使用** Markdown 的 # 符号作为标题。
       - **不要使用** **加粗** 或 *斜体* 符号（保持纯文本整洁）。
       - 仅使用纯文本段落，段落之间用空行分隔。
    """
    
    # Wrap specific instruction to reinforce the rule
    final_instruction = f"{specific_instruction}\n(请直接输出结果，可使用'1.'或'一、'作为层级标题，不要包含其他解释)"

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": f"{final_instruction}\n\n【用户文本】\n{request.text}"}
    ]
    
    # Include user's custom context/requirements if provided
    if request.context and request.context.strip():
        messages[1]["content"] += f"\n\n【额外要求】\n{request.context}"
    
    return StreamingResponse(stream_llm_response(messages), media_type="text/event-stream")

# Helper to avoid code duplication
def stream_llm_response(messages, initial_event=None):
    # Send initial event if provided (e.g., citations)
    if initial_event:
        yield f"data: {json.dumps(initial_event)}\n\n"
        
    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json",
        "HTTP-Referer": "https://localhost:5173",
        "X-Title": "LLM Research Assistant"
    }
    data = {
        "model": MODEL_NAME,
        "messages": messages,
        "stream": True
    }
    try:
        # Timeout: (Connect, Read)
        response = requests.post(BASE_URL, headers=headers, json=data, stream=True, timeout=(10, 180))
        if response.status_code != 200:
             yield f"data: Error: API returned {response.status_code}\n\n"
             yield "data: [DONE]\n\n"
             return

        for line in response.iter_lines():
            if line:
                decoded = line.decode('utf-8')
                if decoded.startswith("data: "):
                    data_str = decoded[6:]
                    
                    if data_str.strip() == "[DONE]":
                        yield "data: [DONE]\n\n"
                        break
                    
                    try:
                        data_json = json.loads(data_str)
                        if "choices" in data_json and len(data_json["choices"]) > 0:
                            delta = data_json["choices"][0].get("delta", {})
                            content = delta.get("content", "")
                            if content:
                                # Send plain text, escape newlines for SSE
                                escaped = content.replace('\n', '\\n')
                                yield f"data: {escaped}\n\n"
                    except json.JSONDecodeError:
                        pass
    except Exception as e:
        yield f"data: Error: {str(e)}\n\n"
        yield "data: [DONE]\n\n"

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
