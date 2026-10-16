from tests.conftest import make_question


def test_health(client):
    assert client.get("/api/health").json() == {"status": "ok"}


def test_exam_crud(client):
    r = client.post("/api/exams", json={"name": "  Toán giữa kỳ  ", "subject": "math"})
    assert r.status_code == 201
    exam = r.json()
    assert exam["id"] == "E00001"
    assert exam["name"] == "Toán giữa kỳ"
    assert exam["status"] == "DRAFT_REVIEW"

    assert len(client.get("/api/exams").json()) == 1
    r = client.put(f"/api/exams/{exam['id']}", json={"name": "Toán cuối kỳ"})
    assert r.json()["name"] == "Toán cuối kỳ"
    assert client.delete(f"/api/exams/{exam['id']}").status_code == 204
    r = client.get(f"/api/exams/{exam['id']}")
    assert r.status_code == 404
    assert r.json()["error"]["code"] == "EXAM_NOT_FOUND"


def test_validation_error_is_structured(client):
    r = client.post("/api/exams", json={"name": "   "})
    assert r.status_code == 422
    assert r.json()["error"]["code"] == "VALIDATION_ERROR"


def test_approve_requires_correct_answers(client, exam):
    eid = exam["id"]
    r = client.post(f"/api/exams/{eid}/approve")
    assert r.status_code == 422
    assert r.json()["error"]["code"] == "EXAM_NOT_READY"

    client.post(f"/api/exams/{eid}/questions", json=make_question(correct=None))
    r = client.post(f"/api/exams/{eid}/approve")
    assert r.status_code == 422
    assert "chưa chọn đáp án đúng" in r.json()["error"]["details"]["problems"][0]["message"]


def test_approve_then_edit_returns_to_draft(client, exam):
    eid = exam["id"]
    q = client.post(f"/api/exams/{eid}/questions", json=make_question()).json()
    assert client.post(f"/api/exams/{eid}/approve").json()["status"] == "APPROVED"
    client.put(f"/api/questions/{q['id']}", json={"content": "Edited"})
    assert client.get(f"/api/exams/{eid}").json()["status"] == "DRAFT_REVIEW"
