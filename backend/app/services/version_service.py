from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.errors import NotFoundError
from app.models import ExamVersion
from app.schemas.generation import (
    AnswerKeyEntry, IssueOut, RenderedChoice, RenderedQuestion, VersionDetail, VersionPreview, VersionSummary,
)
from app.services.exam_service import get_exam
from app.services.version_validator import has_errors, validate_exam_version


def get_version(db: Session, version_id: str) -> ExamVersion:
    version = db.get(ExamVersion, version_id)
    if version is None:
        raise NotFoundError("VERSION_NOT_FOUND", f"Không tìm thấy mã đề {version_id}")
    return version


def list_versions(db: Session, exam_id: str) -> list[ExamVersion]:
    get_exam(db, exam_id)
    return list(db.scalars(select(ExamVersion).where(ExamVersion.exam_id == exam_id).order_by(ExamVersion.version_code)))


def version_detail(db: Session, version: ExamVersion) -> VersionDetail:
    issues = validate_exam_version(db, version)
    return VersionDetail(
        **VersionSummary.model_validate(version).model_dump(),
        answer_key=[
            AnswerKeyEntry(number=k.position + 1, question_id=k.question_id,
                           correct_choice_id=k.correct_choice_id, correct_label=k.correct_label)
            for k in version.answer_keys
        ],
        issues=[IssueOut(**i.__dict__) for i in issues],
        is_valid=not has_errors(issues),
    )


def render_version(db: Session, version: ExamVersion) -> VersionPreview:
    """Build the exam exactly as it will be printed, from snapshots + stored mappings."""
    exam = get_exam(db, version.exam_id)
    keys = {k.position: k for k in version.answer_keys}
    questions: list[RenderedQuestion] = []
    for eq in version.exam_questions:
        snap = eq.question_version.snapshot
        by_id = {c["id"]: c for c in snap["choices"]}
        questions.append(RenderedQuestion(
            number=eq.position + 1,
            question_id=eq.question_id,
            content=snap["content"],
            image_ids=snap.get("image_ids", []),
            choices=[RenderedChoice(label=label, choice_id=cid, content=by_id[cid]["content"])
                     for label, cid in sorted(eq.choice_mapping.items())],
            correct_label=keys[eq.position].correct_label,
        ))
    return VersionPreview(version=VersionSummary.model_validate(version), exam_name=exam.name, questions=questions)
