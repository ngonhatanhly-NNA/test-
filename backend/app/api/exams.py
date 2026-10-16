from fastapi import APIRouter, Depends, Query, Response, status
from sqlalchemy.orm import Session

from app.database.connection import get_db
from app.schemas.exam import ExamCreate, ExamOut, ExamUpdate
from app.services import exam_service
from app.services.storage_service import StorageService, get_storage

router = APIRouter(prefix="/api/exams", tags=["exams"])


@router.get("", response_model=list[ExamOut])
def list_exams(skip: int = Query(0, ge=0), limit: int = Query(50, ge=1, le=200), db: Session = Depends(get_db)):
    return exam_service.list_exams(db, skip, limit)


@router.post("", response_model=ExamOut, status_code=status.HTTP_201_CREATED)
def create_exam(data: ExamCreate, db: Session = Depends(get_db)):
    return exam_service.create_exam(db, data)


@router.get("/{exam_id}", response_model=ExamOut)
def get_exam(exam_id: str, db: Session = Depends(get_db)):
    return exam_service.get_exam(db, exam_id)


@router.put("/{exam_id}", response_model=ExamOut)
def update_exam(exam_id: str, data: ExamUpdate, db: Session = Depends(get_db)):
    return exam_service.update_exam(db, exam_id, data)


@router.delete("/{exam_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_exam(exam_id: str, db: Session = Depends(get_db), storage: StorageService = Depends(get_storage)):
    exam_service.delete_exam(db, exam_id, storage)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post("/{exam_id}/approve", response_model=ExamOut)
def approve_exam(exam_id: str, db: Session = Depends(get_db)):
    return exam_service.approve_exam(db, exam_id)
