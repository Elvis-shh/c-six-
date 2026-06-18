from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app.services.report_fetch_service import report_fetch_service

router = APIRouter(prefix="/ai/v1/reports", tags=["reports"])


class FetchReportRequest(BaseModel):
    companyCode: str
    year: int
    reportType: str = "annual"


class ParseReportRequest(BaseModel):
    filePath: str


@router.post("/fetch")
async def fetch_report(request: FetchReportRequest):
    try:
        data = await report_fetch_service.fetch_report(request.companyCode, request.year, request.reportType)
        return {"code": 0, "message": "success", "data": data}
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@router.post("/parse")
async def parse_report(request: ParseReportRequest):
    try:
        data = await report_fetch_service.parse_report_file(request.filePath)
        return {"code": 0, "message": "success", "data": data}
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
