from fastapi import APIRouter, Depends, File, UploadFile, status
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.database.connection import get_db
from app.schemas.exam import UploadResult, UploadWarning
from app.services import import_service
from app.services.ocr_service import OCRService, get_ocr_service
from app.services.storage_service import StorageService, get_storage
from app.utils.files import validate_pdf_upload

router = APIRouter(tags=["upload"])


@router.post("/api/exams/upload", response_model=UploadResult, status_code=status.HTTP_201_CREATED)
def upload_exam(
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    storage: StorageService = Depends(get_storage),
    ocr: OCRService = Depends(get_ocr_service),
):
    max_size = get_settings().max_upload_size
    data = file.file.read(max_size + 1)  # never read more than the limit + 1 byte
    safe_name = validate_pdf_upload(file.filename, data, max_size)
    result = import_service.import_exam_from_pdf(db, storage, ocr, safe_name, data)
    return UploadResult(
        exam=result.exam,
        source=result.source,
        question_count=result.exam.question_count,
        warnings=[UploadWarning(question_id=w.question_id, number=w.number, message=w.message) for w in result.warnings],
    )
