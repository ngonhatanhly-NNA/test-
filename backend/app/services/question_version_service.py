import hashlib
import json
from typing import Any

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models import Question, QuestionVersion
from app.services import id_service


def snapshot_question(q: Question) -> dict[str, Any]:
    return {
        "content": q.content,
        "question_type": q.question_type,
        "has_image": q.has_image,
        "image_ids": q.image_ids,
        "parameterized": q.parameterized,
        "shuffleable": q.shuffleable,
        "choices": [
            {"id": c.id, "original_label": c.original_label, "content": c.content,
             "is_correct": c.is_correct, "pinned": c.pinned}
            for c in q.choices
        ],
    }


def get_or_create_question_version(db: Session, exam_id: str, q: Question) -> QuestionVersion:
    """Reuse the latest identical snapshot, otherwise store a new immutable one."""
    snapshot = snapshot_question(q)
    digest = hashlib.sha256(json.dumps(snapshot, sort_keys=True, ensure_ascii=False).encode()).hexdigest()
    existing = db.scalar(
        select(QuestionVersion).where(QuestionVersion.question_id == q.id, QuestionVersion.content_hash == digest)
    )
    if existing:
        return existing
    latest = db.scalar(select(func.max(QuestionVersion.version_number)).where(QuestionVersion.question_id == q.id)) or 0
    qv = QuestionVersion(
        id=id_service.next_id(db, id_service.QVERSION), exam_id=exam_id, question_id=q.id,
        version_number=latest + 1, content_hash=digest, snapshot=snapshot,
    )
    db.add(qv)
    db.flush()
    return qv
