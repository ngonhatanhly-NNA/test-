from datetime import datetime
from typing import Any

from sqlalchemy import JSON, DateTime, ForeignKey, Integer, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.connection import Base
from app.models.exam import utcnow


class ExamVersion(Base):
    __tablename__ = "exam_versions"
    __table_args__ = (UniqueConstraint("exam_id", "version_code"),)

    id: Mapped[str] = mapped_column(String(16), primary_key=True)  # V00001
    exam_id: Mapped[str] = mapped_column(ForeignKey("exams.id", ondelete="CASCADE"), index=True)
    generation_config_id: Mapped[int] = mapped_column(ForeignKey("generation_configs.id", ondelete="CASCADE"))
    version_code: Mapped[str] = mapped_column(String(16), nullable=False)  # "001"
    version_index: Mapped[int] = mapped_column(Integer, nullable=False)  # index inside its generation run (RNG input)
    seed: Mapped[int] = mapped_column(Integer, nullable=False)
    question_count: Mapped[int] = mapped_column(Integer, nullable=False)
    status: Mapped[str] = mapped_column(String(16), default="GENERATED", nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

    exam_questions: Mapped[list["ExamQuestion"]] = relationship(
        back_populates="version", cascade="all, delete-orphan", order_by="ExamQuestion.position"
    )
    answer_keys: Mapped[list["AnswerKey"]] = relationship(  # noqa: F821
        back_populates="version", cascade="all, delete-orphan", order_by="AnswerKey.position"
    )


class ExamQuestion(Base):
    """One question slot inside a generated version: which question, which snapshot,
    and the generated_label -> original choice ID mapping."""

    __tablename__ = "exam_questions"
    __table_args__ = (UniqueConstraint("exam_version_id", "position"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    exam_version_id: Mapped[str] = mapped_column(ForeignKey("exam_versions.id", ondelete="CASCADE"), index=True)
    position: Mapped[int] = mapped_column(Integer, nullable=False)
    question_id: Mapped[str] = mapped_column(String(16), nullable=False)
    question_version_id: Mapped[str] = mapped_column(ForeignKey("question_versions.id"), nullable=False)
    choice_mapping: Mapped[dict[str, str]] = mapped_column(JSON, nullable=False)
    parameter_values: Mapped[dict[str, Any]] = mapped_column(JSON, default=dict, nullable=False)

    version: Mapped["ExamVersion"] = relationship(back_populates="exam_questions")
    question_version: Mapped["QuestionVersion"] = relationship()  # noqa: F821
