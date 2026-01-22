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
    from rag_service import ingest_document, search_context, build_rag_prompt, delete_document_vectors
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

# Configuration - DeepSeek via OpenRouter
API_KEY = os.getenv("DEEPSEEK_API_KEY")
BASE_URL = "https://openrouter.fans/v1/chat/completions"
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

    def call_api():
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
            # 超时设置：(连接超时, 读取超时)
            # 连接超时 10 秒，读取超时 120 秒（允许 LLM 生成长回复）
            return requests.post(
                BASE_URL, 
                headers=headers, 
                json=data, 
                stream=True, 
                timeout=(10, 120)
            )
        except requests.exceptions.ConnectTimeout:
            print("Connection timeout: Cannot connect to LLM API")
            raise Exception("连接超时：无法连接到 AI 服务，请稍后重试")
        except requests.exceptions.ReadTimeout:
            print("Read timeout: LLM response took too long")
            raise Exception("响应超时：AI 回复时间过长，请尝试更简短的问题")
        except requests.exceptions.ConnectionError:
            print("Connection error: Network issue")
            raise Exception("网络错误：无法连接到 AI 服务，请检查网络连接")
        except Exception as e:
            print(f"Request failed: {e}")
            raise Exception(f"请求失败：{str(e)}")

    try:
        response = await run_in_threadpool(call_api)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to connect: {str(e)}")

    if response.status_code != 200:
        error_text = response.text
        print(f"API Error: {response.status_code} - {error_text}")
        raise HTTPException(status_code=response.status_code, detail=error_text)

    async def generate():
        try:
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
                                    # Send plain text, escape newlines as \\n for SSE compatibility
                                    escaped = content.replace('\n', '\\n')
                                    yield f"data: {escaped}\n\n"
                        except json.JSONDecodeError:
                            pass
        except Exception as e:
            yield f"data: Error: {str(e)}\n\n"
            yield "data: [DONE]\n\n"

    return StreamingResponse(generate(), media_type="text/event-stream")

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
