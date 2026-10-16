from sqlalchemy import ForeignKey, Integer, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.connection import Base


class AnswerKey(Base):
    """Per-version answer, computed from the FINAL choice mapping (never copied from the original)."""

    __tablename__ = "answer_keys"
    __table_args__ = (UniqueConstraint("exam_version_id", "position"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    exam_version_id: Mapped[str] = mapped_column(ForeignKey("exam_versions.id", ondelete="CASCADE"), index=True)
    position: Mapped[int] = mapped_column(Integer, nullable=False)
    question_id: Mapped[str] = mapped_column(String(16), nullable=False)
    correct_choice_id: Mapped[str] = mapped_column(String(16), nullable=False)
    correct_label: Mapped[str] = mapped_column(String(4), nullable=False)

    version: Mapped["ExamVersion"] = relationship(back_populates="answer_keys")  # noqa: F821
