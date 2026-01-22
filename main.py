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
    if request.use_rag and RAG_ENABLED:
        try:
            context_chunks = await run_in_threadpool(
                search_context, 
                request.message, 
                k=4, 
                doc_id=request.doc_id
            )
            if context_chunks:
                system_prompt = build_rag_prompt(request.message, context_chunks)
                print(f"RAG: Found {len(context_chunks)} context chunks")
        except Exception as e:
            print(f"RAG search failed: {e}")
            # Continue without RAG context
    
    # Add system prompt if RAG found context
    if system_prompt:
        messages.append({"role": "system", "content": system_prompt})
    
    # Add conversation history
    for msg in request.history:
        messages.append({"role": msg.role, "content": msg.content})
    messages.append({"role": "user", "content": request.message})

    return StreamingResponse(stream_llm_response(messages), media_type="text/event-stream")

@app.post("/api/v1/analyze/summary")
async def analyze_summary(request: AnalysisRequest):
    """
    Generate a summary for a specific document.
    """
    if not RAG_ENABLED:
        raise HTTPException(status_code=503, detail="RAG service is not available")
    
    print(f"=== Generating summary for: {request.doc_id} ===")
    
    # 1. Get all text chunks
    chunks = await run_in_threadpool(get_document_chunks, request.doc_id)
    if not chunks:
        raise HTTPException(status_code=404, detail=f"No content found for document: {request.doc_id}")
    
    # Simple strategy: Concatenate all chunks (truncate if too long) for now
    # TODO: Implement Map-Reduce for very large docs
    full_text = "\n\n".join(chunks)
    
    # Truncate to avoid context limit (approx 30k chars for ~8k tokens safety, DeepSeek supports 32k-128k but let's be safe)
    if len(full_text) > 30000:
        full_text = full_text[:30000] + "...(truncated)"
    
    # 2. Build Prompt
    system_prompt = """你是一个专业的学术研究助手。请仔细阅读用户提供的文档内容，并生成一份高质量的摘要。
摘要应包含：
1. 核心研究问题
2. 主要方法
3. 关键发现与结论
请使用 Markdown 格式，层级清晰。"""

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": f"请总结以下文档内容：\n\n{full_text}"}
    ]
    
    # 3. Call LLM (Non-streaming for now, or streaming? Let's use streaming for better UX)
    # Re-using the call_api logic pattern but tailored for this endpoint
    
    # ... Helper function to stream ...
    return StreamingResponse(stream_llm_response(messages), media_type="text/event-stream")

@app.post("/api/v1/analyze/comparison")
async def analyze_comparison(request: ComparisonRequest):
    """
    Compare multiple documents.
    """
    if not RAG_ENABLED:
        raise HTTPException(status_code=503, detail="RAG service is not available")
        
    doc_contents = []
    for doc_id in request.doc_ids:
        chunks = await run_in_threadpool(get_document_chunks, doc_id)
        if chunks:
            text = "\n\n".join(chunks)[:15000] # Limit each doc to ensure fit
            doc_contents.append(f"【文档: {doc_id}】\n{text}")
            
    if not doc_contents:
        raise HTTPException(status_code=404, detail="No content found for provided documents")
        
    combined_text = "\n\n====================\n\n".join(doc_contents)
    
    system_prompt = """你是一个专业的学术情报分析师。请对比阅读以下多篇文档，并进行深度对比分析。
请重点关注：
1. 各文档观点的异同
2. 研究方法的区别
3. 结论的互补性或冲突
请生成一份结构化的对比报告。"""

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": f"请对比以下文档：\n\n{combined_text}"}
    ]
    
    return StreamingResponse(stream_llm_response(messages), media_type="text/event-stream")

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
    
    system_prompt = "你是一个资深的学术写作导师，旨在帮助用户写出高水平的学术文章。"
    
    # Build user message with optional context
    user_message = f"{specific_instruction}\n\n【用户文本】\n{request.text}"
    
    # Include user's custom context/requirements if provided
    if request.context and request.context.strip():
        user_message += f"\n\n【额外要求】\n请务必遵循以下额外要求：{request.context}"
    
    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": user_message}
    ]
    
    return StreamingResponse(stream_llm_response(messages), media_type="text/event-stream")

# Helper to avoid code duplication
def stream_llm_response(messages):
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
