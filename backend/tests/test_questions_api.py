from tests.conftest import make_question


def test_create_question_assigns_immutable_ids(client, exam):
    r = client.post(f"/api/exams/{exam['id']}/questions", json=make_question(correct=1))
    assert r.status_code == 201
    q = r.json()
    assert q["id"] == "Q00001"
    assert [c["id"] for c in q["choices"]] == ["C00001", "C00002", "C00003", "C00004"]
    assert [c["original_label"] for c in q["choices"]] == ["A", "B", "C", "D"]
    assert q["correct_choice_id"] == "C00002"  # stored as a choice ID, not a letter
    assert q["original_number"] == 1


def test_reject_multiple_correct_and_too_few_choices(client, exam):
    url = f"/api/exams/{exam['id']}/questions"
    bad = make_question()
    bad["choices"][0]["is_correct"] = True  # now two correct
    assert client.post(url, json=bad).status_code == 422
    assert client.post(url, json=make_question(n=1)).status_code == 422


def test_update_keeps_choice_ids_and_changes_correct(client, exam):
    q = client.post(f"/api/exams/{exam['id']}/questions", json=make_question(correct=0)).json()
    choices = [
        {"id": c["id"], "content": c["content"], "is_correct": c["id"] == q["choices"][2]["id"]}
        for c in q["choices"]
    ]
    r = client.put(f"/api/questions/{q['id']}", json={"choices": choices})
    assert r.status_code == 200
    out = r.json()
    assert [c["id"] for c in out["choices"]] == [c["id"] for c in q["choices"]]
    assert out["correct_choice_id"] == q["choices"][2]["id"]


def test_update_can_add_and_remove_choices(client, exam):
    q = client.post(f"/api/exams/{exam['id']}/questions", json=make_question(correct=0)).json()
    keep = q["choices"][:2]
    choices = [
        {"id": keep[0]["id"], "content": "kept A", "is_correct": True},
        {"id": keep[1]["id"], "content": "kept B"},
        {"content": "brand new"},
    ]
    out = client.put(f"/api/questions/{q['id']}", json={"choices": choices}).json()
    assert len(out["choices"]) == 3
    assert out["choices"][2]["id"] == "C00005"  # old IDs are never reused
    assert out["choices"][2]["original_label"] == "C"


def test_update_rejects_foreign_choice_id(client, exam):
    eid = exam["id"]
    q1 = client.post(f"/api/exams/{eid}/questions", json=make_question()).json()
    q2 = client.post(f"/api/exams/{eid}/questions", json=make_question()).json()
    choices = [
        {"id": q2["choices"][0]["id"], "content": "x", "is_correct": True},
        {"content": "y"},
    ]
    r = client.put(f"/api/questions/{q1['id']}", json={"choices": choices})
    assert r.status_code == 422
    assert r.json()["error"]["code"] == "CHOICE_NOT_IN_QUESTION"


def test_delete_question_does_not_reuse_ids_and_repacks_positions(client, exam):
    eid = exam["id"]
    for i in range(3):
        client.post(f"/api/exams/{eid}/questions", json=make_question(content=f"Q{i}"))
    assert client.delete("/api/questions/Q00002").status_code == 204
    qs = client.get(f"/api/exams/{eid}/questions").json()
    assert [(q["id"], q["position"]) for q in qs] == [("Q00001", 0), ("Q00003", 1)]
    new = client.post(f"/api/exams/{eid}/questions", json=make_question()).json()
    assert new["id"] == "Q00004"
    assert new["position"] == 2


def test_reorder_questions(client, exam):
    eid = exam["id"]
    for i in range(3):
        client.post(f"/api/exams/{eid}/questions", json=make_question(content=f"Q{i}"))
    r = client.put(f"/api/exams/{eid}/questions/order", json={"question_ids": ["Q00003", "Q00001", "Q00002"]})
    assert r.status_code == 200
    assert [q["id"] for q in r.json()] == ["Q00003", "Q00001", "Q00002"]
    assert [q["original_number"] for q in r.json()] == [3, 1, 2]  # metadata untouched

    bad = client.put(f"/api/exams/{eid}/questions/order", json={"question_ids": ["Q00001"]})
    assert bad.status_code == 422
    assert bad.json()["error"]["code"] == "INVALID_QUESTION_ORDER"


def test_flags_shuffleable_pinned_parameterized(client, exam):
    body = make_question(parameterized=True, shuffleable=False)
    body["choices"][3]["pinned"] = True
    q = client.post(f"/api/exams/{exam['id']}/questions", json=body).json()
    assert q["parameterized"] is True and q["shuffleable"] is False
    assert q["choices"][3]["pinned"] is True


def test_delete_exam_cascades(client, exam):
    client.post(f"/api/exams/{exam['id']}/questions", json=make_question())
    assert client.delete(f"/api/exams/{exam['id']}").status_code == 204
    assert client.get("/api/questions/Q00001").status_code == 404
