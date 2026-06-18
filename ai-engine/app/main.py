from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api import chat, ner, ocr, rag
from app.core.config import settings

app = FastAPI(title=settings.app_name)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(rag.router)
app.include_router(chat.router)
app.include_router(ocr.router)
app.include_router(ner.router)


@app.get("/health")
async def health():
    return {"status": "ok"}
