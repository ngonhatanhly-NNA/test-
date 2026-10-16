import logging

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.errors import AppError, ConflictError, NotFoundError
from app.models import Exam, ExamStatus
from app.schemas.exam import ExamCreate, ExamUpdate
from app.services import id_service
from app.services.storage_service import StorageService

logger = logging.getLogger("app.exam")


def get_exam(db: Session, exam_id: str) -> Exam:
    exam = db.get(Exam, exam_id)
    if exam is None:
        raise NotFoundError("EXAM_NOT_FOUND", f"Không tìm thấy đề thi {exam_id}")
    return exam


def list_exams(db: Session, skip: int = 0, limit: int = 50) -> list[Exam]:
    stmt = select(Exam).order_by(Exam.created_at.desc(), Exam.id.desc()).offset(skip).limit(limit)
    return list(db.scalars(stmt))


def create_exam(db: Session, data: ExamCreate) -> Exam:
    exam = Exam(id=id_service.next_id(db, id_service.EXAM), **data.model_dump())
    exam.status = ExamStatus.DRAFT_REVIEW.value
    db.add(exam)
    db.commit()
    logger.info("exam created id=%s", exam.id)
    return exam


def update_exam(db: Session, exam_id: str, data: ExamUpdate) -> Exam:
    exam = get_exam(db, exam_id)
    for key, value in data.model_dump(exclude_unset=True).items():
        setattr(exam, key, value)
    db.commit()
    return exam


def delete_exam(db: Session, exam_id: str, storage: StorageService) -> None:
    exam = get_exam(db, exam_id)
    if exam.status == ExamStatus.GENERATING.value:
        raise ConflictError("EXAM_BUSY", "Đề thi đang được tạo mã đề, không thể xóa")
    keys = [i.storage_key for q in exam.questions for i in q.images]
    if exam.source_key:
        keys.append(exam.source_key)
    db.delete(exam)
    db.commit()
    for key in keys:
        try:
            storage.delete_file(key)
        except OSError:
            logger.warning("could not delete file %s", key)
    logger.info("exam deleted id=%s", exam_id)


def collect_approval_problems(exam: Exam) -> list[dict[str, str]]:
    problems: list[dict[str, str]] = []
    if not exam.questions:
        problems.append({"question_id": "", "message": "Đề thi chưa có câu hỏi nào"})
    for q in exam.questions:
        if len(q.choices) < 2:
            problems.append({"question_id": q.id, "message": "Câu hỏi cần ít nhất 2 đáp án"})
        if q.correct_choice_id is None:
            problems.append({"question_id": q.id, "message": "Câu hỏi chưa chọn đáp án đúng"})
    return problems


def approve_exam(db: Session, exam_id: str) -> Exam:
    exam = get_exam(db, exam_id)
    if exam.status == ExamStatus.GENERATING.value:
        raise ConflictError("EXAM_BUSY", "Đề thi đang được tạo mã đề")
    problems = collect_approval_problems(exam)
    if problems:
        raise AppError(
            "EXAM_NOT_READY",
            f"Còn {len(problems)} vấn đề cần sửa trước khi duyệt đề",
            422,
            {"problems": problems},
        )
    exam.status = ExamStatus.APPROVED.value
    db.commit()
    logger.info("exam approved id=%s", exam_id)
    return exam
