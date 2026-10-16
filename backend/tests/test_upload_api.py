from tests.conftest import FakeOCR, make_pdf, make_png
from app.main import app
from app.services.ocr_service import get_ocr_service

QUESTIONS = [
    "Question 1. What is 2+2?", "A. 3", "B. 4", "C. 5", "D. 6",
    "Question 2. Look at the figure:", make_png(), "A. one", "B. two", "C. three", "D. four",
    "Question 3. Pick one?", "A. x", "B. y", "C. z", "D. w",
    "ANSWER KEY", "1. B", "2. D",
]


def upload(client, data, name="de thi.pdf"):
    return client.post("/api/exams/upload", files={"file": (name, data, "application/pdf")})


def test_upload_text_pdf_creates_draft_exam(client):
    r = upload(client, make_pdf([QUESTIONS]))
    assert r.status_code == 201, r.text
    body = r.json()
    assert body["source"] == "text" and body["question_count"] == 3
    assert body["exam"]["status"] == "DRAFT_REVIEW" and body["exam"]["name"] == "de thi"

    qs = client.get(f"/api/exams/{body['exam']['id']}/questions").json()
    assert [q["original_number"] for q in qs] == [1, 2, 3]
    assert [c["content"] for c in qs[0]["choices"]] == ["3", "4", "5", "6"]
    assert qs[0]["correct_choice_id"] == qs[0]["choices"][1]["id"]
    assert qs[1]["correct_choice_id"] == qs[1]["choices"][3]["id"]
    assert qs[2]["correct_choice_id"] is None and "Chưa có đáp án đúng" in qs[2]["import_warnings"]
    assert any(w["question_id"] == qs[2]["id"] for w in body["warnings"])


def test_image_is_linked_to_question_and_served(client):
    qs = client.get(f"/api/exams/{upload(client, make_pdf([QUESTIONS])).json()['exam']['id']}/questions").json()
    assert qs[0]["image_ids"] == [] and qs[1]["has_image"] is True and len(qs[1]["image_ids"]) == 1
    img = client.get(f"/api/images/{qs[1]['image_ids'][0]}")
    assert img.status_code == 200 and img.headers["content-type"] == "image/png"
    assert img.content.startswith(b"\x89PNG")


def test_editing_a_question_clears_import_warnings(client):
    eid = upload(client, make_pdf([QUESTIONS])).json()["exam"]["id"]
    q = client.get(f"/api/exams/{eid}/questions").json()[2]
    out = client.put(f"/api/questions/{q['id']}", json={"content": "Pick one!"}).json()
    assert out["import_warnings"] == []


def test_delete_question_removes_image_file(client):
    eid = upload(client, make_pdf([QUESTIONS])).json()["exam"]["id"]
    q = client.get(f"/api/exams/{eid}/questions").json()[1]
    assert client.delete(f"/api/questions/{q['id']}").status_code == 204
    assert client.get(f"/api/images/{q['image_ids'][0]}").status_code == 404


def test_rejects_non_pdf_and_bad_content(client):
    assert upload(client, b"hello", "notes.txt").status_code == 415
    r = upload(client, b"not really a pdf", "fake.pdf")
    assert r.status_code == 415 and r.json()["error"]["code"] == "INVALID_FILE_TYPE"
    assert upload(client, b"", "empty.pdf").status_code == 422


def test_rejects_oversized_file(client, monkeypatch):
    from app.core.config import get_settings
    monkeypatch.setattr(get_settings(), "max_upload_size", 100)
    r = upload(client, make_pdf([QUESTIONS]))
    assert r.status_code == 413 and r.json()["error"]["code"] == "FILE_TOO_LARGE"


def test_corrupt_pdf_and_no_questions_leave_no_exam(client):
    assert upload(client, b"%PDF-1.4 garbage").json()["error"]["code"] == "PDF_INVALID"
    r = upload(client, make_pdf([["Hello world, this page has plenty of text but no questions at all."]]))
    assert r.status_code == 422 and r.json()["error"]["code"] == "QUESTION_PARSE_FAILED"
    assert client.get("/api/exams").json() == []


def test_scanned_pdf_uses_ocr_abstraction(client):
    scan = make_pdf([[make_png(400, 400)]])  # image only, no text layer
    r = upload(client, scan)
    assert r.status_code == 422 and r.json()["error"]["code"] == "OCR_NOT_AVAILABLE"

    app.dependency_overrides[get_ocr_service] = lambda: FakeOCR("Cau 1. Scanned?\nA. a\nB. b\nC. c\nD. d")
    try:
        r = upload(client, scan)
    finally:
        app.dependency_overrides.pop(get_ocr_service)
    assert r.status_code == 201 and r.json()["source"] == "ocr" and r.json()["question_count"] == 1
