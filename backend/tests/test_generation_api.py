from tests.conftest import approved_exam, make_question


def generate(client, eid, **cfg):
    body = {"number_of_versions": 10, "seed": 12345, **cfg}
    return client.post(f"/api/exams/{eid}/generate", json=body)


def original_state(client, eid):
    return client.get(f"/api/exams/{eid}/questions").json()


def test_generate_requires_approval(client):
    eid = client.post("/api/exams", json={"name": "x"}).json()["id"]
    r = generate(client, eid)
    assert r.status_code == 409 and r.json()["error"]["code"] == "EXAM_NOT_APPROVED"


def test_generate_10_versions(client):
    eid = approved_exam(client, 8)
    r = generate(client, eid)
    assert r.status_code == 201, r.text
    body = r.json()
    assert [v["version_code"] for v in body["versions"]] == [f"{i:03d}" for i in range(1, 11)]
    assert all(v["question_count"] == 8 for v in body["versions"])
    assert body["config"]["seed"] == 12345
    assert client.get(f"/api/exams/{eid}").json()["status"] == "GENERATED"
    assert client.get(f"/api/exams/{eid}").json()["version_count"] == 10
    assert len(client.get(f"/api/exams/{eid}/versions").json()) == 10


def test_answer_key_is_derived_from_final_mapping(client):
    """Independently recompute every answer from the original exam + the preview."""
    eid = approved_exam(client, 10)
    original = {q["id"]: q for q in original_state(client, eid)}
    versions = generate(client, eid).json()["versions"]
    orders = set()
    for v in versions:
        preview = client.get(f"/api/versions/{v['id']}/preview").json()
        detail = client.get(f"/api/versions/{v['id']}").json()
        assert detail["is_valid"] is True and detail["issues"] == []
        orders.add(tuple(q["question_id"] for q in preview["questions"]))
        for q, key in zip(preview["questions"], detail["answer_key"]):
            orig = original[q["question_id"]]
            correct_text = next(c["content"] for c in orig["choices"] if c["id"] == orig["correct_choice_id"])
            shown = {c["label"]: c["content"] for c in q["choices"]}
            assert shown[q["correct_label"]] == correct_text  # label points at the same choice
            assert key["correct_label"] == q["correct_label"]
            assert key["correct_choice_id"] == orig["correct_choice_id"]  # invariant: same Choice ID
    assert len(orders) > 1  # question orders actually differ


def test_same_seed_reproduces_exactly_and_different_seed_differs(client):
    eid = approved_exam(client, 10)

    def fingerprint(**cfg):
        out = []
        for v in generate(client, eid, **cfg).json()["versions"]:
            p = client.get(f"/api/versions/{v['id']}/preview").json()
            out.append([(q["question_id"], [c["choice_id"] for c in q["choices"]]) for q in p["questions"]])
        return out

    a, b = fingerprint(seed=42), fingerprint(seed=42)
    assert a == b
    assert a != fingerprint(seed=43)


def test_codes_continue_across_generations(client):
    eid = approved_exam(client, 4)
    generate(client, eid, number_of_versions=2)
    codes = [v["version_code"] for v in generate(client, eid, number_of_versions=2).json()["versions"]]
    assert codes == ["003", "004"]


def test_server_picks_seed_when_missing_and_it_is_reproducible(client):
    eid = approved_exam(client, 6)
    r = client.post(f"/api/exams/{eid}/generate", json={"number_of_versions": 1}).json()
    seed = r["config"]["seed"]
    again = generate(client, eid, number_of_versions=1, seed=seed).json()
    first = client.get(f"/api/versions/{r['versions'][0]['id']}/preview").json()["questions"]
    second = client.get(f"/api/versions/{again['versions'][0]['id']}/preview").json()["questions"]
    assert [q["question_id"] for q in first] == [q["question_id"] for q in second]


def test_no_shuffle_options_keep_original_layout(client):
    eid = approved_exam(client, 5)
    r = generate(client, eid, number_of_versions=2, shuffle_questions=False, shuffle_choices=False).json()
    orig = original_state(client, eid)
    p = client.get(f"/api/versions/{r['versions'][0]['id']}/preview").json()["questions"]
    assert [q["question_id"] for q in p] == [q["id"] for q in orig]
    assert [[c["label"] for c in q["choices"]] for q in p] == [["A", "B", "C", "D"]] * 5
    assert [c["choice_id"] for c in p[0]["choices"]] == [c["id"] for c in orig[0]["choices"]]
    assert any("giống" in w for w in r["warnings"])  # identical versions are reported


def test_original_exam_is_not_mutated(client):
    eid = approved_exam(client, 6)
    before = original_state(client, eid)
    generate(client, eid)
    assert original_state(client, eid) == before


def test_editing_after_generation_does_not_change_existing_versions(client):
    eid = approved_exam(client, 4)
    v = generate(client, eid, number_of_versions=1).json()["versions"][0]
    before = client.get(f"/api/versions/{v['id']}/preview").json()
    q = original_state(client, eid)[0]
    client.put(f"/api/questions/{q['id']}", json={"content": "COMPLETELY CHANGED"})
    assert client.get(f"/api/exams/{eid}").json()["status"] == "DRAFT_REVIEW"
    assert client.get(f"/api/versions/{v['id']}/preview").json() == before
    assert generate(client, eid).status_code == 409  # must re-approve first


def test_pinned_choice_and_non_shuffleable_question(client):
    eid = client.post("/api/exams", json={"name": "pins"}).json()["id"]
    body = make_question(correct=0)
    body["choices"][3] = {"content": "Tất cả đáp án trên", "pinned": True}
    client.post(f"/api/exams/{eid}/questions", json=body)
    client.post(f"/api/exams/{eid}/questions", json=make_question(correct=1, shuffleable=False))
    for _ in range(3):
        client.post(f"/api/exams/{eid}/questions", json=make_question(correct=2))
    client.post(f"/api/exams/{eid}/approve")
    r = generate(client, eid, number_of_versions=20).json()
    for v in r["versions"]:
        qs = client.get(f"/api/versions/{v['id']}/preview").json()["questions"]
        pinned_q = next(q for q in qs if q["question_id"] == "Q00001")
        assert pinned_q["choices"][3]["content"] == "Tất cả đáp án trên"
        fixed = qs[1]
        assert fixed["question_id"] == "Q00002"  # keeps its position
        assert [c["content"] for c in fixed["choices"]] == ["opt 0", "opt 1", "opt 2", "opt 3"]


def test_change_parameters_not_supported_yet(client):
    eid = approved_exam(client, 3)
    r = generate(client, eid, change_parameters=True)
    assert r.status_code == 422 and r.json()["error"]["code"] == "PARAMETERS_NOT_SUPPORTED"
    assert client.get(f"/api/exams/{eid}/versions").json() == []


def test_validator_detects_corruption_and_deleted_image(client):
    from app.database.connection import get_db
    from app.main import app
    from app.models import AnswerKey, ExamVersion
    from tests.conftest import make_pdf, make_png

    eid = approved_exam(client, 3)
    vid = generate(client, eid, number_of_versions=1).json()["versions"][0]["id"]

    db = next(app.dependency_overrides[get_db]())
    key = db.query(AnswerKey).filter_by(exam_version_id=vid, position=0).one()
    key.correct_label = "A" if key.correct_label != "A" else "B"  # corrupt the answer key
    db.commit()
    detail = client.get(f"/api/versions/{vid}").json()
    assert detail["is_valid"] is False
    assert detail["issues"][0]["code"] == "ANSWER_MAPPING_INVALID"

    # image removed after generation -> flagged as a validation error
    pdf = make_pdf([["Question 1. See:", make_png(), "A. a", "B. b", "C. c", "D. d", "Answer: A"]])
    up = client.post("/api/exams/upload", files={"file": ("i.pdf", pdf, "application/pdf")}).json()
    client.post(f"/api/exams/{up['exam']['id']}/approve")
    v2 = generate(client, up["exam"]["id"], number_of_versions=1).json()["versions"][0]["id"]
    assert client.get(f"/api/versions/{v2}").json()["is_valid"] is True
    db.query(__import__("app.models", fromlist=["QuestionImage"]).QuestionImage).delete()
    db.commit()
    codes = [i["code"] for i in client.get(f"/api/versions/{v2}").json()["issues"]]
    assert "IMAGE_MISSING" in codes
    db.close()


def test_delete_exam_removes_versions(client):
    eid = approved_exam(client, 3)
    vid = generate(client, eid, number_of_versions=2).json()["versions"][0]["id"]
    assert client.delete(f"/api/exams/{eid}").status_code == 204
    assert client.get(f"/api/versions/{vid}").status_code == 404
