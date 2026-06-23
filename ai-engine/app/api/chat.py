import json
import re

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


def _format_source(source: str, company_name: str) -> str:
    """将来源名规范化为 公司名_xxxx年年报 或保持知识库名称"""
    if not source:
        return f"{company_name}财报"
    if "常识库" in source or "知识库" in source:
        return source
    year_match = re.search(r"(\d{4})\s*年", source)
    if year_match:
        return f"{company_name}_{year_match.group(1)}年年报"
    pdf_year = re.search(r"(\d{4})", source)
    if pdf_year and len(pdf_year.group(1)) == 4 and pdf_year.group(1).startswith(("19", "20")):
        return f"{company_name}_{pdf_year.group(1)}年年报"
    return f"{company_name}财报"


@router.post("/generate")
async def generate(request: GenerateRequest):
    async def stream():
        # Buffer all tokens first to strip [FOLLOWUPS] before sending
        full_content = ""
        async for token in llm_service.chat_stream(request.companyName, request.message, request.ragContext):
            full_content += token

        # Parse [FOLLOWUPS] section and strip from content
        clean_content, follow_ups = _parse_followups(full_content)

        # Send cleaned content as one token (since we already buffered)
        yield f"data: {json.dumps({'type': 'token', 'content': clean_content}, ensure_ascii=False)}\n\n"

        # Send AI-generated follow-ups
        if follow_ups:
            yield f"data: {json.dumps({'type': 'followups', 'followUps': follow_ups}, ensure_ascii=False)}\n\n"

        refs = [{
            "source": _format_source(item.get("source", ""), request.companyName),
            "snippet": item.get("content", "")[:120],
            "score": item.get("score", 0),
            "page": item.get("page"),
        } for item in request.ragContext]
        yield f"data: {json.dumps({'type': 'refs', 'refs': refs}, ensure_ascii=False)}\n\n"
        yield f"data: {json.dumps({'type': 'done'})}\n\n"

    return StreamingResponse(stream(), media_type="text/event-stream; charset=utf-8")


def _parse_followups(text: str) -> tuple[str, list[str]]:
    """Extract [FOLLOWUPS] section from LLM response."""
    marker = "[FOLLOWUPS]"
    idx = text.find(marker)
    if idx < 0:
        return text, []
    before = text[:idx].strip()
    after = text[idx + len(marker):].strip()
    lines = [line.strip() for line in after.split("\n") if line.strip()]
    # Take first 3 non-empty lines as follow-up questions
    follow_ups = [line for line in lines if len(line) > 2 and ("？" in line or "?" in line)][:3]
    if not follow_ups:
        follow_ups = lines[:3]
    return before, follow_ups
