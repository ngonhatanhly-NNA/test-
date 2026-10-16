from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.id_counter import IdCounter

EXAM, QUESTION, CHOICE, IMAGE, VERSION, QVERSION = "E", "Q", "C", "I", "V", "QV"


def next_id(db: Session, prefix: str) -> str:
    """Return the next public ID for `prefix` (e.g. Q00001). IDs are never reused,
    even after deletion. Locks the counter row on PostgreSQL."""
    row = db.execute(
        select(IdCounter).where(IdCounter.name == prefix).with_for_update()
    ).scalar_one_or_none()
    if row is None:
        row = IdCounter(name=prefix, value=0)
        db.add(row)
        db.flush()
    row.value += 1
    db.flush()
    return f"{prefix}{row.value:05d}"
