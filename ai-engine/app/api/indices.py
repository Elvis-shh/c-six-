from fastapi import APIRouter, HTTPException

from app.services.index_service import index_service

router = APIRouter(prefix="/ai/v1/indices", tags=["indices"])


@router.get("/csi300/constituents")
async def csi300_constituents():
    try:
        data = await index_service.get_csi300_constituents()
        return {"code": 0, "message": "success", "data": data}
    except Exception as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc


@router.get("/csi-a50/constituents")
async def csi_a50_constituents():
    try:
        data = await index_service.get_csi_a50_constituents()
        return {"code": 0, "message": "success", "data": data}
    except Exception as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
