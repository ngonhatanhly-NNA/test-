import enum
from datetime import datetime, timezone

from sqlalchemy import DateTime, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.connection import Base


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


class ExamStatus(str, enum.Enum):
    UPLOADED = "UPLOADED"
    PARSING = "PARSING"
    DRAFT_REVIEW = "DRAFT_REVIEW"
    APPROVED = "APPROVED"
    GENERATING = "GENERATING"
    GENERATED = "GENERATED"
    EXPORTED = "EXPORTED"


class Exam(Base):
    __tablename__ = "exams"

    id: Mapped[str] = mapped_column(String(16), primary_key=True)
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    subject: Mapped[str | None] = mapped_column(String(64), nullable=True)
    status: Mapped[str] = mapped_column(String(16), default=ExamStatus.DRAFT_REVIEW.value, nullable=False)
    source_filename: Mapped[str | None] = mapped_column(String(255), nullable=True)
    source_key: Mapped[str | None] = mapped_column(String(255), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow)

    questions: Mapped[list["Question"]] = relationship(  # noqa: F821
        back_populates="exam",
        cascade="all, delete-orphan",
        order_by="Question.position",
    )

    versions: Mapped[list["ExamVersion"]] = relationship(  # noqa: F821
        cascade="all, delete-orphan", order_by="ExamVersion.version_code"
    )
    question_versions: Mapped[list["QuestionVersion"]] = relationship(  # noqa: F821
        cascade="all, delete-orphan"
    )
    generation_configs: Mapped[list["GenerationConfig"]] = relationship(  # noqa: F821
        cascade="all, delete-orphan"
    )

    @property
    def question_count(self) -> int:
        return len(self.questions)

    @property
    def version_count(self) -> int:
        return len(self.versions)
