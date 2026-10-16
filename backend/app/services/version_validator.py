"""validate_exam_version(): run before export. Any issue with severity 'error' blocks export."""
from dataclasses import dataclass

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import ExamVersion, QuestionImage


@dataclass
class Issue:
    severity: str  # "error" | "warning"
    code: str
    message: str
    question_id: str | None = None
    position: int | None = None


def validate_exam_version(db: Session, version: ExamVersion) -> list[Issue]:
    issues: list[Issue] = []

    def add(sev: str, code: str, msg: str, qid: str | None = None, pos: int | None = None) -> None:
        issues.append(Issue(sev, code, msg, qid, pos))

    eqs = list(version.exam_questions)
    keys = {k.position: k for k in version.answer_keys}

    if len(eqs) != version.question_count:
        add("error", "QUESTION_COUNT_MISMATCH", f"Có {len(eqs)} câu, cấu hình yêu cầu {version.question_count} câu")
    if len(keys) != len(version.answer_keys) or {e.position for e in eqs} != set(keys):
        add("error", "ANSWER_KEY_INCOMPLETE", "Đáp án không khớp với danh sách câu hỏi")
    if sorted(e.position for e in eqs) != list(range(len(eqs))):
        add("error", "POSITION_GAP", "Thứ tự câu hỏi không liên tục")

    seen: set[str] = set()
    for eq in eqs:
        if eq.question_id in seen:
            add("error", "DUPLICATE_QUESTION", "Câu hỏi bị lặp trong mã đề", eq.question_id, eq.position + 1)
        seen.add(eq.question_id)

    all_image_ids = {i for eq in eqs for i in eq.question_version.snapshot.get("image_ids", [])}
    existing_images = (
        set(db.scalars(select(QuestionImage.id).where(QuestionImage.id.in_(all_image_ids)))) if all_image_ids else set()
    )

    for eq in eqs:
        snap = eq.question_version.snapshot
        n, qid = eq.position + 1, eq.question_id
        choices = snap["choices"]
        ids = [c["id"] for c in choices]

        if len(choices) < 2:
            add("error", "MISSING_CHOICES", "Câu hỏi có ít hơn 2 đáp án", qid, n)
        if sorted(eq.choice_mapping.values()) != sorted(ids):
            add("error", "INVALID_CHOICE_MAPPING", "Thứ tự đáp án không khớp với đáp án gốc", qid, n)

        correct = [c["id"] for c in choices if c["is_correct"]]
        if len(correct) != 1:
            add("error", "CORRECT_ANSWER_COUNT", f"Câu hỏi có {len(correct)} đáp án đúng (cần đúng 1)", qid, n)
        else:
            key = keys.get(eq.position)
            if key is None or key.question_id != qid:
                add("error", "ANSWER_KEY_MISSING", "Thiếu đáp án cho câu này", qid, n)
            elif key.correct_choice_id != correct[0] or eq.choice_mapping.get(key.correct_label) != correct[0]:
                add("error", "ANSWER_MAPPING_INVALID", "Đáp án không khớp với thứ tự đáp án đã trộn", qid, n)

        contents = [c["content"].strip().lower() for c in choices]
        if len(set(contents)) != len(contents):
            add("warning", "DUPLICATE_CHOICES", "Có các đáp án trùng nội dung", qid, n)

        if snap.get("has_image") and not snap.get("image_ids"):
            add("error", "IMAGE_MISSING", "Câu hỏi có hình nhưng không còn hình nào được gắn", qid, n)
        for image_id in snap.get("image_ids", []):
            if image_id not in existing_images:
                add("error", "IMAGE_MISSING", f"Hình {image_id} đã bị xóa", qid, n)
    return issues


def has_errors(issues: list[Issue]) -> bool:
    return any(i.severity == "error" for i in issues)
