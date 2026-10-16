import pytest

from app.services.answer_mapping_service import find_correct_label, label_choices
from app.services.shuffle_service import QuestionPlanInput, derive_rng, plan_version, shuffle_with_fixed


def test_mandatory_answer_after_choice_shuffle():
    """Original Q1 answer B. After shuffle: A=orig C, B=orig D, C=orig B, D=orig A -> answer C."""
    original = {"A": "C1", "B": "C2", "C": "C3", "D": "C4"}
    correct_choice_id = original["B"]  # == "C2"
    displayed_order = [original["C"], original["D"], original["B"], original["A"]]

    mapping = label_choices(displayed_order)
    assert mapping == {"A": "C3", "B": "C4", "C": "C2", "D": "C1"}
    assert find_correct_label(mapping, correct_choice_id) == "C"


def test_find_correct_label_missing_choice_raises():
    with pytest.raises(ValueError):
        find_correct_label({"A": "C1"}, "C9")


def test_label_choices_rejects_duplicates():
    with pytest.raises(ValueError):
        label_choices(["C1", "C1"])


def _inputs(n=10):
    return [
        QuestionPlanInput(f"Q{i}", True, tuple((f"C{i}{k}", False) for k in range(4))) for i in range(n)
    ]


def test_same_seed_same_plan_and_different_index_differs():
    a = plan_version(_inputs(), 12345, 0, True, True)
    assert a == plan_version(_inputs(), 12345, 0, True, True)
    assert a != plan_version(_inputs(), 12345, 1, True, True)
    assert a != plan_version(_inputs(), 999, 0, True, True)


def test_invariant_choice_ids_preserved_for_any_shuffle():
    qs = _inputs(8)
    for seed in range(50):
        plan = plan_version(qs, seed, seed % 7, True, True)
        assert sorted(p.question_id for p in plan) == sorted(q.question_id for q in qs)
        originals = {q.question_id: sorted(c for c, _ in q.choices) for q in qs}
        for p in plan:
            assert sorted(p.choice_order) == originals[p.question_id]


def test_no_shuffle_keeps_original_order():
    qs = _inputs(5)
    plan = plan_version(qs, 1, 0, False, False)
    assert [p.question_id for p in plan] == [q.question_id for q in qs]
    assert [p.choice_order for p in plan] == [tuple(c for c, _ in q.choices) for q in qs]


def test_non_shuffleable_question_keeps_position_and_choice_order():
    qs = _inputs(6)
    qs[2] = QuestionPlanInput("Q2", False, qs[2].choices)
    for seed in range(30):
        plan = plan_version(qs, seed, 0, True, True)
        assert plan[2].question_id == "Q2"
        assert plan[2].choice_order == tuple(c for c, _ in qs[2].choices)


def test_pinned_choice_keeps_its_slot():
    choices = (("Ca", False), ("Cb", False), ("Cc", False), ("Cd", True))  # "Tất cả đáp án trên"
    qs = [QuestionPlanInput("Q1", True, choices)]
    for seed in range(30):
        assert plan_version(qs, seed, 0, False, True)[0].choice_order[3] == "Cd"


def test_shuffle_with_fixed_is_a_permutation():
    out = shuffle_with_fixed(list("abcdef"), [False, True, False, False, True, False], derive_rng(1))
    assert out[1] == "b" and out[4] == "e"
    assert sorted(out) == list("abcdef")


def test_plan_does_not_mutate_inputs():
    qs = _inputs(5)
    snapshot = list(qs)
    plan_version(qs, 1, 0, True, True)
    assert qs == snapshot
