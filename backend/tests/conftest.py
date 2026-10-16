import os

os.environ.setdefault("DATABASE_URL", "sqlite://")

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

import app.models  # noqa: F401  (register tables)
from app.database.connection import Base, get_db
from app.main import app
from app.services.ocr_service import OCRService, get_ocr_service
from app.services.storage_service import LocalStorageService, get_storage


@pytest.fixture()
def client(tmp_path):
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    Session = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)

    def override():
        db = Session()
        try:
            yield db
        finally:
            db.close()

    app.dependency_overrides[get_db] = override
    app.dependency_overrides[get_storage] = lambda: LocalStorageService(tmp_path / 'storage')
    with TestClient(app, raise_server_exceptions=False) as c:
        yield c
    app.dependency_overrides.clear()


def make_question(content="Q?", correct=1, n=4, **extra):
    return {
        "content": content,
        "choices": [{"content": f"opt {i}", "is_correct": i == correct} for i in range(n)],
        **extra,
    }


@pytest.fixture()
def exam(client):
    return client.post("/api/exams", json={"name": "Physics Midterm"}).json()


class FakeOCR(OCRService):
    def __init__(self, text: str) -> None:
        self.text = text

    def extract_text(self, image_png: bytes) -> str:
        assert image_png.startswith(b"\x89PNG")
        return self.text


def make_pdf(pages: list[list[str | bytes]]) -> bytes:
    """Build a text PDF. A bytes item is a PNG image placed inline."""
    import pymupdf

    doc = pymupdf.open()
    for items in pages:
        page = doc.new_page()
        y = 72
        for item in items:
            if isinstance(item, bytes):
                page.insert_image(pymupdf.Rect(72, y, 172, y + 60), stream=item)
                y += 70
            else:
                page.insert_text((72, y), item, fontsize=11)
                y += 18
    return doc.tobytes()


def make_png(w: int = 80, h: int = 60) -> bytes:
    import pymupdf

    pix = pymupdf.Pixmap(pymupdf.csRGB, pymupdf.IRect(0, 0, w, h), False)
    pix.clear_with(200)
    return pix.tobytes("png")


def approved_exam(client, n_questions=5, **q_extra):
    eid = client.post("/api/exams", json={"name": "Gen test"}).json()["id"]
    for i in range(n_questions):
        client.post(f"/api/exams/{eid}/questions", json=make_question(content=f"Question {i}", correct=i % 4, **q_extra))
    assert client.post(f"/api/exams/{eid}/approve").status_code == 200
    return eid
