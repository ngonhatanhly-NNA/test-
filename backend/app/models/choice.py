from sqlalchemy import Boolean, ForeignKey, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.connection import Base


class Choice(Base):
    __tablename__ = "choices"

    id: Mapped[str] = mapped_column(String(16), primary_key=True)  # immutable: C00001
    question_id: Mapped[str] = mapped_column(ForeignKey("questions.id", ondelete="CASCADE"), index=True)
    position: Mapped[int] = mapped_column(Integer, nullable=False)
    original_label: Mapped[str] = mapped_column(String(4), nullable=False)  # A, B, C, D (display only)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    is_correct: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    # pinned choices keep their slot when choices are shuffled ("Cả A và B đều đúng", ...)
    pinned: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)

    question: Mapped["Question"] = relationship(back_populates="choices")  # noqa: F821
