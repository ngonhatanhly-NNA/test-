import logging
from dataclasses import dataclass, field
from pathlib import Path

from sqlalchemy.orm import Session

from app.core.errors import AppError
from app.models import Choice, Exam, ExamStatus, Question, QuestionImage
from app.services import id_service
from app.services.ocr_service import OCRService
from app.services.pdf_parser import extract_document
from app.services.question_parser import LABELS, parse_questions
from app.services.storage_service import StorageService

logger = logging.getLogger("app.import")

MIME = {"png": "image/png", "jpeg": "image/jpeg"}


@dataclass
class ImportWarning:
    question_id: str | None
    number: int | None
    message: str


@dataclass
class ImportResult:
    exam: Exam
    source: str
    warnings: list[ImportWarning] = field(default_factory=list)


def import_exam_from_pdf(
    db: Session, storage: StorageService, ocr: OCRService, filename: str, data: bytes
) -> ImportResult:
    name = Path(filename).stem[:255] or "Đề thi mới"
    exam = Exam(id=id_service.next_id(db, id_service.EXAM), name=name, source_filename=filename)
    exam.status = ExamStatus.UPLOADED.value
    db.add(exam)
    db.flush()
    saved: list[str] = []
    try:
        exam.source_key = storage.save_file(data, f"exams/{exam.id}/source.pdf")
        saved.append(exam.source_key)
        exam.status = ExamStatus.PARSING.value
        db.flush()

        document = extract_document(data, ocr)
        parsed = parse_questions(document.items)
        if not parsed.questions:
            raise AppError(
                "QUESTION_PARSE_FAILED",
                "Không nhận diện được câu hỏi nào. Kiểm tra định dạng 'Câu 1.' / 'A.' trong PDF.",
                422,
            )

        result = ImportResult(exam, document.source)
        result.warnings.extend(ImportWarning(None, None, w) for w in parsed.warnings)

        for position, pq in enumerate(parsed.questions):
            question = Question(
                id=id_service.next_id(db, id_service.QUESTION),
                exam_id=exam.id,
                original_number=pq.number,
                position=position,
                content=pq.content,
                import_warnings=pq.warnings,
                has_image=bool(pq.image_indexes),
            )
            db.add(question)
            for index, pc in enumerate(pq.choices):
                question.choices.append(
                    Choice(
                        id=id_service.next_id(db, id_service.CHOICE),
                        position=index,
                        original_label=LABELS[index],
                        content=pc.content,
                        is_correct=pc.is_correct,
                    )
                )
            for index, image_no in enumerate(pq.image_indexes):
                img = document.images[image_no]
                image_id = id_service.next_id(db, id_service.IMAGE)
                key = storage.save_file(img.data, f"exams/{exam.id}/images/{image_id}.{img.ext}")
                saved.append(key)
                question.images.append(
                    QuestionImage(
                        id=image_id, position=index, storage_key=key, mime_type=MIME[img.ext],
                        width=img.width, height=img.height, page=img.page,
                    )
                )
            result.warnings.extend(ImportWarning(question.id, pq.number, w) for w in pq.warnings)

        exam.status = ExamStatus.DRAFT_REVIEW.value
        db.commit()
        logger.info("import done exam=%s questions=%s source=%s", exam.id, len(parsed.questions), document.source)
        return result
    except Exception:
        db.rollback()
        for key in saved:
            try:
                storage.delete_file(key)
            except OSError:
                logger.warning("could not clean up %s", key)
        raise
