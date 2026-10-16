"""Invariant: the correct answer always references the same Choice ID; only its label changes."""
from collections.abc import Sequence

LABELS = "ABCDEFGHIJ"


def label_choices(choice_order: Sequence[str]) -> dict[str, str]:
    """generated_label -> original choice ID, following the displayed order."""
    if len(choice_order) > len(LABELS):
        raise ValueError("Too many choices")
    if len(set(choice_order)) != len(choice_order):
        raise ValueError("Duplicate choice ID in order")
    return {LABELS[i]: cid for i, cid in enumerate(choice_order)}


def find_correct_label(choice_mapping: dict[str, str], correct_choice_id: str) -> str:
    for label, choice_id in choice_mapping.items():
        if choice_id == correct_choice_id:
            return label
    raise ValueError(f"Correct choice {correct_choice_id} is missing from the choice mapping")
