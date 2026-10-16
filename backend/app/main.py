import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api import exams, generation, questions, upload
from app.core.config import get_settings
from app.core.errors import register_error_handlers

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")

settings = get_settings()

app = FastAPI(title=settings.app_name, docs_url=None if settings.is_production else "/docs")
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_methods=["*"],
    allow_headers=["*"],
)
register_error_handlers(app)
app.include_router(upload.router)
app.include_router(exams.router)
app.include_router(generation.router)
app.include_router(questions.router)


@app.get("/api/health", tags=["system"])
def health() -> dict[str, str]:
    return {"status": "ok"}
