from datetime import datetime

from sqlalchemy import Boolean, DateTime, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.database.connection import Base
from app.models.exam import utcnow


class GenerationConfig(Base):
    __tablename__ = "generation_configs"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    exam_id: Mapped[str] = mapped_column(ForeignKey("exams.id", ondelete="CASCADE"), index=True)
    number_of_versions: Mapped[int] = mapped_column(Integer, nullable=False)
    shuffle_questions: Mapped[bool] = mapped_column(Boolean, nullable=False)
    shuffle_choices: Mapped[bool] = mapped_column(Boolean, nullable=False)
    change_parameters: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    difficulty_mode: Mapped[str] = mapped_column(String(16), default="same", nullable=False)
    seed: Mapped[int] = mapped_column(Integer, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
