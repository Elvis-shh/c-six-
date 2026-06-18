from fastapi import APIRouter
from pydantic import BaseModel

from app.services.rag_service import rag_service

router = APIRouter(prefix="/ai/v1/rag", tags=["rag"])


class SearchRequest(BaseModel):
    query: str
    companyCode: str
    topK: int = 5


@router.post("/search")
async def search(request: SearchRequest):
    results = rag_service.search(request.query, request.companyCode, request.topK)
    return {"code": 0, "message": "success", "data": [item.__dict__ for item in results]}
