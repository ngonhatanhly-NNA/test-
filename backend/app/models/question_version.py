from datetime import datetime
from typing import Any

from sqlalchemy import JSON, DateTime, ForeignKey, Integer, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.database.connection import Base
from app.models.exam import utcnow


class QuestionVersion(Base):
    """Immutable snapshot of a question (content + choices) taken when versions are generated.
    Later edits to the live question never change generated exams."""

    __tablename__ = "question_versions"
    __table_args__ = (UniqueConstraint("question_id", "version_number"),)

    id: Mapped[str] = mapped_column(String(16), primary_key=True)  # QV00001
    exam_id: Mapped[str] = mapped_column(ForeignKey("exams.id", ondelete="CASCADE"), index=True)
    question_id: Mapped[str] = mapped_column(String(16), index=True)  # no FK: survives question deletion
    version_number: Mapped[int] = mapped_column(Integer, nullable=False)
    content_hash: Mapped[str] = mapped_column(String(64), nullable=False)
    snapshot: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    # Thêm vào class QuestionVersion:
    rendered_content = Column(Text, nullable=True)
    parameter_values = Column(JSON, nullable=True)
    rendered_choices = Column(JSON, nullable=True)
