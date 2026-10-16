from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.orm import Session

from app.database.connection import get_db
from app.schemas.question import QuestionCreate, QuestionOut, QuestionUpdate, ReorderRequest
from app.core.errors import NotFoundError
from app.models import QuestionImage
from app.services import question_service
from app.services.storage_service import StorageService, get_storage

router = APIRouter(tags=["questions"])


@router.get("/api/exams/{exam_id}/questions", response_model=list[QuestionOut])
def list_questions(exam_id: str, db: Session = Depends(get_db)):
    return question_service.list_questions(db, exam_id)


@router.post("/api/exams/{exam_id}/questions", response_model=QuestionOut, status_code=status.HTTP_201_CREATED)
def create_question(exam_id: str, data: QuestionCreate, db: Session = Depends(get_db)):
    return question_service.create_question(db, exam_id, data)


@router.put("/api/exams/{exam_id}/questions/order", response_model=list[QuestionOut])
def reorder_questions(exam_id: str, data: ReorderRequest, db: Session = Depends(get_db)):
    return question_service.reorder_questions(db, exam_id, data.question_ids)


@router.get("/api/questions/{question_id}", response_model=QuestionOut)
def get_question(question_id: str, db: Session = Depends(get_db)):
    return question_service.get_question(db, question_id)


@router.put("/api/questions/{question_id}", response_model=QuestionOut)
def update_question(question_id: str, data: QuestionUpdate, db: Session = Depends(get_db)):
    return question_service.update_question(db, question_id, data)


@router.delete("/api/questions/{question_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_question(question_id: str, db: Session = Depends(get_db), storage: StorageService = Depends(get_storage)):
    question_service.delete_question(db, question_id, storage)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.get("/api/images/{image_id}")
def get_image(image_id: str, db: Session = Depends(get_db), storage: StorageService = Depends(get_storage)):
    image = db.get(QuestionImage, image_id)
    if image is None:
        raise NotFoundError("IMAGE_NOT_FOUND", f"Không tìm thấy hình {image_id}")
    try:
        data = storage.get_file(image.storage_key)
    except FileNotFoundError:
        raise NotFoundError("IMAGE_NOT_FOUND", f"Không tìm thấy hình {image_id}") from None
    return Response(content=data, media_type=image.mime_type, headers={"Cache-Control": "private, max-age=3600"})
