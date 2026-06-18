from fastapi import APIRouter
from pydantic import BaseModel

from app.services.ner_service import ner_service

router = APIRouter(prefix="/ai/v1/ner", tags=["ner"])


class ExtractRequest(BaseModel):
    text: str


@router.post("/extract")
async def extract(request: ExtractRequest):
    return {"code": 0, "message": "success", "data": await ner_service.extract(request.text)}
