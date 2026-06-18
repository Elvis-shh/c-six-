import json

from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

from app.services.llm_service import llm_service

router = APIRouter(prefix="/ai/v1/chat", tags=["chat"])


class GenerateRequest(BaseModel):
    companyCode: str
    companyName: str
    industry: str = "未知行业"
    message: str
    ragContext: list[dict] = []


@router.post("/generate")
async def generate(request: GenerateRequest):
    async def stream():
        yield f"data: {json.dumps({'type': 'thinking', 'content': '正在分析财报上下文...'})}\n\n"
        async for token in llm_service.chat_stream(request.companyName, request.message, request.ragContext):
            yield f"data: {json.dumps({'type': 'token', 'content': token}, ensure_ascii=False)}\n\n"
        refs = [{"source": item.get("source"), "snippet": item.get("content", "")[:80], "score": item.get("score", 0)} for item in request.ragContext]
        yield f"data: {json.dumps({'type': 'refs', 'refs': refs}, ensure_ascii=False)}\n\n"
        yield f"data: {json.dumps({'type': 'done'})}\n\n"

    return StreamingResponse(stream(), media_type="text/event-stream")
