from datetime import datetime
from typing import Literal
from typing import Any, Dict, Optional
from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

QuestionType = Literal["multiple_choice"]
MIN_CHOICES = 2
MAX_CHOICES = 10


def _clean(value: str) -> str:
    return value.strip()

class VariableRangeSpec(BaseModel):
    min: float = 1.0
    max: float = 10.0
    step: float = 1.0

class ParameterConfigSchema(BaseModel):
    enabled: bool = False
    template: Optional[str] = None
    variables: Dict[str, VariableRangeSpec] = {}
    correct_formula: str = ""
    distractor_formulas: list[str] = []
    decimals: int = 2
    unit: str = ""
    
class ChoiceIn(BaseModel):
    """Choice payload. `id` set -> update that choice; no `id` -> create a new one."""

    id: str | None = None
    content: str = Field(min_length=1)
    is_correct: bool = False
    pinned: bool = False

    _strip = field_validator("content")(_clean)

    @field_validator("content")
    @classmethod
    def _not_blank(cls, v: str) -> str:
        if not v:
            raise ValueError("Nội dung đáp án không được để trống")
        return v


class ChoiceOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    question_id: str
    position: int
    original_label: str
    content: str
    is_correct: bool
    pinned: bool


def _check_choices(choices: list[ChoiceIn]) -> None:
    if not MIN_CHOICES <= len(choices) <= MAX_CHOICES:
        raise ValueError(f"Câu hỏi cần từ {MIN_CHOICES} đến {MAX_CHOICES} đáp án")
    if sum(c.is_correct for c in choices) > 1:
        raise ValueError("Mỗi câu hỏi chỉ được có một đáp án đúng")
    ids = [c.id for c in choices if c.id]
    if len(ids) != len(set(ids)):
        raise ValueError("Trùng ID đáp án")


class QuestionCreate(BaseModel):
    content: str = Field(min_length=1)
    question_type: QuestionType = "multiple_choice"
    original_number: int | None = None
    subject: str | None = None
    chapter: str | None = None
    difficulty: int | None = Field(default=None, ge=1, le=5)
    has_image: bool = False
    parameterized: bool = False
    shuffleable: bool = True
    choices: list[ChoiceIn]

    @field_validator("content")
    @classmethod
    def _content(cls, v: str) -> str:
        v = v.strip()
        if not v:
            raise ValueError("Nội dung câu hỏi không được để trống")
        return v

    @model_validator(mode="after")
    def _choices(self) -> "QuestionCreate":
        _check_choices(self.choices)
        return self


class QuestionUpdate(BaseModel):
    """Partial update. If `choices` is sent it replaces the full list (matched by `id`)."""

    content: str | None = Field(default=None, min_length=1)
    original_number: int | None = None
    subject: str | None = None
    chapter: str | None = None
    difficulty: int | None = Field(default=None, ge=1, le=5)
    has_image: bool | None = None
    parameterized: bool | None = None
    shuffleable: bool | None = None
    choices: list[ChoiceIn] | None = None
    parameter_config: Optional[Dict[str, Any]] = None

    @field_validator("content")
    @classmethod
    def _content(cls, v: str | None) -> str | None:
        if v is None:
            return v
        v = v.strip()
        if not v:
            raise ValueError("Nội dung câu hỏi không được để trống")
        return v

    @model_validator(mode="after")
    def _choices(self) -> "QuestionUpdate":
        if self.choices is not None:
            _check_choices(self.choices)
        return self


class QuestionOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    exam_id: str
    original_number: int | None
    position: int
    content: str
    question_type: str
    subject: str | None
    chapter: str | None
    difficulty: int | None
    has_image: bool
    parameterized: bool
    shuffleable: bool
    correct_choice_id: str | None
    image_ids: list[str]
    import_warnings: list[str]
    choices: list[ChoiceOut]
    created_at: datetime
    updated_at: datetime
    parameter_config: Optional[Dict[str, Any]] = None

    @field_validator("import_warnings", mode="before")
    @classmethod
    def _warnings(cls, v: list[str] | None) -> list[str]:
        return v or []


class ReorderRequest(BaseModel):
    question_ids: list[str]
