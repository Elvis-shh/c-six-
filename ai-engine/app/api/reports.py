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


class ExtractQuotesRequest(BaseModel):
    filePath: str


class BatchFetchParseRequest(BaseModel):
    companyCode: str
    years: int = 5  # how many recent years to fetch


@router.post("/batch-fetch-parse")
async def batch_fetch_parse(request: BatchFetchParseRequest):
    """Fetch last N years of annual reports for a company, parse each one."""
    import datetime
    results = []
    current_year = datetime.date.today().year
    # Start from last year (current year's report may not be published yet)
    for offset in range(request.years):
        year = current_year - 1 - offset
        try:
            fetch_data = await report_fetch_service.fetch_report(request.companyCode, year, "annual")
            file_path = fetch_data.get("filePath", "")
            parse_data = await report_fetch_service.parse_report_file(file_path)
            results.append({
                "year": year,
                "status": "ok",
                "filePath": file_path,
                "indicators": parse_data.get("extractedData", {}),
            })
        except Exception as e:
            results.append({"year": year, "status": "error", "error": str(e)})
    return {"code": 0, "message": "success", "data": results}


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


@router.post("/quotes")
async def extract_quotes(request: ExtractQuotesRequest):
    try:
        data = await report_fetch_service.extract_quote_chunks(request.filePath)
        return {"code": 0, "message": "success", "data": data}
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
