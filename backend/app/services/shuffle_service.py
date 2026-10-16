"""Deterministic, side-effect-free shuffling. Nothing here touches the database.

Rules:
- Randomness comes only from derive_rng(seed, ...), so same inputs + seed => same output.
- A question with shuffleable=False keeps its position AND its choice order.
- A pinned choice keeps its slot while the other choices are shuffled around it.
"""
import hashlib
import random
from collections.abc import Sequence
from dataclasses import dataclass
from typing import TypeVar

T = TypeVar("T")


def derive_rng(seed: int, *parts: object) -> random.Random:
    """Independent RNG stream per (seed, parts...), e.g. (seed, version_index, "questions")."""
    material = ":".join([str(seed), *map(str, parts)]).encode()
    return random.Random(int.from_bytes(hashlib.sha256(material).digest()[:8], "big"))


def _fisher_yates(items: list[T], rng: random.Random) -> None:
    for i in range(len(items) - 1, 0, -1):
        j = rng.randrange(i + 1)
        items[i], items[j] = items[j], items[i]


def shuffle_with_fixed(items: Sequence[T], fixed: Sequence[bool], rng: random.Random) -> list[T]:
    """Shuffle only the non-fixed items among the non-fixed slots; fixed items stay put."""
    if len(items) != len(fixed):
        raise ValueError("items and fixed must have the same length")
    free_slots = [i for i, is_fixed in enumerate(fixed) if not is_fixed]
    movable = [items[i] for i in free_slots]
    _fisher_yates(movable, rng)
    result = list(items)
    for slot, value in zip(free_slots, movable):
        result[slot] = value
    return result


@dataclass(frozen=True)
class QuestionPlanInput:
    question_id: str
    shuffleable: bool
    choices: tuple[tuple[str, bool], ...]  # (choice_id, pinned) in original order


@dataclass(frozen=True)
class PlannedQuestion:
    question_id: str
    choice_order: tuple[str, ...]  # choice IDs in the order they are displayed (A, B, C, ...)


def plan_version(
    questions: Sequence[QuestionPlanInput],
    seed: int,
    version_index: int,
    shuffle_questions: bool,
    shuffle_choices: bool,
) -> list[PlannedQuestion]:
    """Decide question order and choice order for one version. Inputs are never mutated."""
    if shuffle_questions:
        rng = derive_rng(seed, version_index, "questions")
        ordered = shuffle_with_fixed(questions, [not q.shuffleable for q in questions], rng)
    else:
        ordered = list(questions)

    planned: list[PlannedQuestion] = []
    for q in ordered:
        ids = [cid for cid, _ in q.choices]
        if shuffle_choices and q.shuffleable:
            rng = derive_rng(seed, version_index, "choices", q.question_id)
            ids = shuffle_with_fixed(ids, [pinned for _, pinned in q.choices], rng)
        planned.append(PlannedQuestion(q.question_id, tuple(ids)))
    return planned
