from datetime import datetime

from sqlalchemy import JSON, Boolean, DateTime, ForeignKey, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.connection import Base
from app.models.exam import utcnow


class Question(Base):
    __tablename__ = "questions"

    id: Mapped[str] = mapped_column(String(16), primary_key=True)  # immutable: Q00001
    exam_id: Mapped[str] = mapped_column(ForeignKey("exams.id", ondelete="CASCADE"), index=True)
    original_number: Mapped[int | None] = mapped_column(Integer, nullable=True)  # metadata only
    position: Mapped[int] = mapped_column(Integer, nullable=False)  # current order in the exam
    content: Mapped[str] = mapped_column(Text, nullable=False)
    question_type: Mapped[str] = mapped_column(String(32), default="multiple_choice", nullable=False)
    subject: Mapped[str | None] = mapped_column(String(64), nullable=True)
    chapter: Mapped[str | None] = mapped_column(String(128), nullable=True)
    difficulty: Mapped[int | None] = mapped_column(Integer, nullable=True)
    has_image: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    parameterized: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    shuffleable: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    import_warnings: Mapped[list[str] | None] = mapped_column(JSON, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow)
    parameter_config = Column(JSON, nullable=True, default=None)

    exam: Mapped["Exam"] = relationship(back_populates="questions")  # noqa: F821
    choices: Mapped[list["Choice"]] = relationship(  # noqa: F821
        back_populates="question",
        cascade="all, delete-orphan",
        order_by="Choice.position",
    )

    images: Mapped[list["QuestionImage"]] = relationship(  # noqa: F821
        back_populates="question", cascade="all, delete-orphan", order_by="QuestionImage.position"
    )

    @property
    def image_ids(self) -> list[str]:
        return [i.id for i in self.images]

    @property
    def correct_choice_id(self) -> str | None:
        """Derived from Choice.is_correct, so there is a single source of truth."""
        for choice in self.choices:
            if choice.is_correct:
                return choice.id
        return None
