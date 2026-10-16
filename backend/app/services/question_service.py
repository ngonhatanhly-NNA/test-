import logging
import string

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.errors import AppError, ConflictError, NotFoundError
from app.models import Choice, Exam, ExamStatus, Question
from app.schemas.question import ChoiceIn, QuestionCreate, QuestionUpdate
from app.services import id_service
from app.services.exam_service import get_exam
from app.services.storage_service import StorageService

logger = logging.getLogger("app.question")

LABELS = string.ascii_uppercase


def _delete_files(storage: StorageService, keys: list[str]) -> None:
    for key in keys:
        try:
            storage.delete_file(key)
        except OSError:
            logger.warning("could not delete file %s", key)


def get_question(db: Session, question_id: str) -> Question:
    question = db.get(Question, question_id)
    if question is None:
        raise NotFoundError("QUESTION_NOT_FOUND", f"Không tìm thấy câu hỏi {question_id}")
    return question


def list_questions(db: Session, exam_id: str) -> list[Question]:
    get_exam(db, exam_id)
    stmt = select(Question).where(Question.exam_id == exam_id).order_by(Question.position)
    return list(db.scalars(stmt))


def _touch_exam(exam: Exam) -> None:
    """Editing questions invalidates a previous approval (generated versions keep
    their own snapshots, added in Phase 4)."""
    if exam.status == ExamStatus.GENERATING.value:
        raise ConflictError("EXAM_BUSY", "Đề thi đang được tạo mã đề, không thể chỉnh sửa")
    if exam.status != ExamStatus.DRAFT_REVIEW.value:
        exam.status = ExamStatus.DRAFT_REVIEW.value


def _apply_choices(db: Session, question: Question, items: list[ChoiceIn]) -> None:
    existing = {c.id: c for c in question.choices}
    unknown = [i.id for i in items if i.id and i.id not in existing]
    if unknown:
        raise AppError(
            "CHOICE_NOT_IN_QUESTION", "Đáp án không thuộc câu hỏi này", 422, {"choice_ids": unknown}
        )
    keep_ids = {i.id for i in items if i.id}
    for cid, choice in existing.items():
        if cid not in keep_ids:
            question.choices.remove(choice)  # delete-orphan removes the row

    ordered: list[Choice] = []
    for index, item in enumerate(items):
        if item.id:
            choice = existing[item.id]
        else:
            choice = Choice(id=id_service.next_id(db, id_service.CHOICE))
            question.choices.append(choice)
        choice.position = index
        choice.original_label = LABELS[index]
        choice.content = item.content
        choice.is_correct = item.is_correct
        choice.pinned = item.pinned
        ordered.append(choice)
    question.choices.sort(key=lambda c: c.position)


def create_question(db: Session, exam_id: str, data: QuestionCreate) -> Question:
    exam = get_exam(db, exam_id)
    _touch_exam(exam)
    position = max((q.position for q in exam.questions), default=-1) + 1
    payload = data.model_dump(exclude={"choices"})
    question = Question(id=id_service.next_id(db, id_service.QUESTION), exam_id=exam.id, position=position, **payload)
    if question.original_number is None:
        question.original_number = position + 1
    db.add(question)
    _apply_choices(db, question, data.choices)
    db.commit()
    logger.info("question created id=%s exam=%s", question.id, exam.id)
    return question


def update_question(db: Session, question_id: str, data: QuestionUpdate) -> Question:
    question = get_question(db, question_id)
    _touch_exam(question.exam)
    changes = data.model_dump(exclude_unset=True, exclude={"choices"})
    question.import_warnings = []  # the teacher has reviewed this question
    for key, value in changes.items():
        setattr(question, key, value)
    if data.choices is not None:
        _apply_choices(db, question, data.choices)
    db.commit()
    return question


def delete_question(db: Session, question_id: str, storage: StorageService) -> None:
    question = get_question(db, question_id)
    exam = question.exam
    _touch_exam(exam)
    image_keys = [i.storage_key for i in question.images]
    db.delete(question)
    db.flush()
    db.refresh(exam)
    for index, q in enumerate(exam.questions):
        q.position = index
    db.commit()
    _delete_files(storage, image_keys)
    logger.info("question deleted id=%s", question_id)


def reorder_questions(db: Session, exam_id: str, question_ids: list[str]) -> list[Question]:
    exam = get_exam(db, exam_id)
    current = {q.id: q for q in exam.questions}
    if len(question_ids) != len(set(question_ids)) or set(question_ids) != set(current):
        raise AppError(
            "INVALID_QUESTION_ORDER",
            "Danh sách câu hỏi phải gồm đúng tất cả câu hỏi của đề, không trùng lặp",
            422,
        )
    _touch_exam(exam)
    for index, qid in enumerate(question_ids):
        current[qid].position = index
    db.commit()
    db.refresh(exam)
    return list(exam.questions)
