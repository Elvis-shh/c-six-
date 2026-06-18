from fastapi import APIRouter, UploadFile, File

from app.services.ner_service import ner_service
from app.services.ocr_service import ocr_service

router = APIRouter(prefix="/ai/v1/ocr", tags=["ocr"])


@router.post("/parse")
async def parse(file: UploadFile = File(...)):
    content = await file.read()
    text = ocr_service.parse_text(file.filename or "report", content)
    extracted = await ner_service.extract(text)
    return {"code": 0, "message": "success", "data": {"text": text, "extractedData": extracted}}
