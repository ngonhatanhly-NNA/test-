import logging
import secrets
from dataclasses import dataclass, field

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.errors import AppError, ConflictError
from app.models import AnswerKey, ExamQuestion, ExamStatus, ExamVersion, GenerationConfig
from app.schemas.generation import GenerationConfigIn
from app.services import id_service
from app.services.answer_mapping_service import find_correct_label, label_choices
from app.services.exam_service import collect_approval_problems, get_exam
from app.services.question_version_service import get_or_create_question_version
from app.services.shuffle_service import QuestionPlanInput, plan_version
from app.services.version_validator import has_errors, validate_exam_version
from app.services.parameter_service import ParameterService

logger = logging.getLogger("app.generator")

GENERATABLE = {ExamStatus.APPROVED.value, ExamStatus.GENERATED.value, ExamStatus.EXPORTED.value}


@dataclass
class GenerationOutcome:
    config: GenerationConfig
    versions: list[ExamVersion]
    warnings: list[str] = field(default_factory=list)


def _next_code_number(db: Session, exam_id: str) -> int:
    codes = db.scalars(select(ExamVersion.version_code).where(ExamVersion.exam_id == exam_id))
    return max((int(c) for c in codes if c.isdigit()), default=0) + 1


def generate_exam_versions(db: Session, exam_id: str, config: GenerationConfigIn) -> GenerationOutcome:
    """Create N versions from an approved exam. Original questions are never modified:
    each version stores question snapshots, an order and a label->choice mapping."""
    exam = get_exam(db, exam_id)
    if config.vary_parameters and getattr(q, "parameter_config", None):
        rendered_content, processed_choices, sampled_params = ParameterService.apply_parameters(
            question_content=q.content,
            choices=raw_choices,
            parameter_config=q.parameter_config,
            rng=rng,
        )
    else:
        rendered_content = q.content
        processed_choices = [dict(c) for c in raw_choices]
        sampled_params = {}
    if exam.status not in GENERATABLE:
        raise ConflictError("EXAM_NOT_APPROVED", "Cần duyệt đề thi trước khi tạo mã đề")
    problems = collect_approval_problems(exam)
    if problems:
        raise AppError("EXAM_NOT_READY", "Đề thi còn câu hỏi chưa hợp lệ", 422, {"problems": problems})
    if config.change_parameters:
        raise AppError("PARAMETERS_NOT_SUPPORTED", "Tính năng thay đổi số liệu chưa được hỗ trợ", 422)

    seed = config.seed if config.seed is not None else secrets.randbelow(2**31)
    logger.info("generate start exam=%s versions=%s seed=%s", exam_id, config.number_of_versions, seed)

    try:
        exam.status = ExamStatus.GENERATING.value
        questions = list(exam.questions)
        snapshots = {q.id: get_or_create_question_version(db, exam.id, q) for q in questions}
        correct_ids = {q.id: q.correct_choice_id for q in questions}
        plan_inputs = [
            QuestionPlanInput(q.id, q.shuffleable, tuple((c.id, c.pinned) for c in q.choices)) for q in questions
        ]

        cfg = GenerationConfig(
            exam_id=exam.id, number_of_versions=config.number_of_versions,
            shuffle_questions=config.shuffle_questions, shuffle_choices=config.shuffle_choices,
            change_parameters=False, difficulty_mode=config.difficulty_mode, seed=seed,
        )
        db.add(cfg)
        db.flush()

        outcome = GenerationOutcome(cfg, [])
        start = _next_code_number(db, exam.id)
        fingerprints: dict[tuple, str] = {}

        for index in range(config.number_of_versions):
            plan = plan_version(plan_inputs, seed, index, config.shuffle_questions, config.shuffle_choices)
            version = ExamVersion(
                id=id_service.next_id(db, id_service.VERSION), exam_id=exam.id, generation_config_id=cfg.id,
                version_code=f"{start + index:03d}", version_index=index, seed=seed,
                question_count=len(plan), status="GENERATED",
            )
            for position, item in enumerate(plan):
                mapping = label_choices(item.choice_order)
                correct_id = correct_ids[item.question_id]
                version.exam_questions.append(ExamQuestion(
                    position=position, question_id=item.question_id,
                    question_version=snapshots[item.question_id], choice_mapping=mapping, parameter_values={},
                ))
                version.answer_keys.append(AnswerKey(
                    position=position, question_id=item.question_id, correct_choice_id=correct_id,
                    correct_label=find_correct_label(mapping, correct_id),
                ))
            db.add(version)
            db.flush()

            issues = validate_exam_version(db, version)
            if has_errors(issues):
                raise AppError(
                    "VERSION_VALIDATION_FAILED", f"Mã đề {version.version_code} không hợp lệ", 422,
                    {"issues": [i.__dict__ for i in issues if i.severity == "error"]},
                )

            fingerprint = tuple((p.question_id, p.choice_order) for p in plan)
            if fingerprint in fingerprints:
                outcome.warnings.append(
                    f"Mã đề {version.version_code} có thứ tự giống mã đề {fingerprints[fingerprint]} "
                    "(đề quá ít câu/đáp án để tạo thêm thứ tự khác)"
                )
            else:
                fingerprints[fingerprint] = version.version_code
            outcome.versions.append(version)

        exam.status = ExamStatus.GENERATED.value
        db.commit()
    except Exception:
        db.rollback()
        raise
    logger.info("generate done exam=%s versions=%s", exam_id, len(outcome.versions))
    return outcome
