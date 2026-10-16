from sqlalchemy import Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.database.connection import Base


class IdCounter(Base):
    """Monotonic counters for public IDs (E00001, Q00001, C00001). Never reused."""

    __tablename__ = "id_counters"

    name: Mapped[str] = mapped_column(String(8), primary_key=True)
    value: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
