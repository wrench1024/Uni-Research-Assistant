import os
import json
import uvicorn
import requests
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
from fastapi.responses import StreamingResponse
from starlette.concurrency import run_in_threadpool

from dotenv import load_dotenv

# Load environment variables
load_dotenv()

app = FastAPI()

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

@app.get("/")
def read_root():
    return {"status": "ok", "service": "LLM Research Assistant AI Service (DeepSeek)"}

@app.post("/api/v1/chat/stream")
async def stream_chat(request: ChatRequest):
    print(f"=== Received chat request ===")
    print(f"Message: {request.message}")
    print(f"History count: {len(request.history)}")

    # Build messages with history
    messages = []
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
            return requests.post(BASE_URL, headers=headers, json=data, stream=True, timeout=60)
        except Exception as e:
            print(f"Request failed: {e}")
            raise e

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