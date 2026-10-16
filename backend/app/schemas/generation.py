from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class GenerationConfigIn(BaseModel):
    number_of_versions: int = Field(default=10, ge=1, le=50)
    shuffle_questions: bool = True
    shuffle_choices: bool = True
    change_parameters: bool = False
    difficulty_mode: Literal["same"] = "same"
    seed: int | None = Field(default=None, ge=0, lt=2**31)  # omitted -> server picks and returns one


class GenerationConfigOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    exam_id: str
    number_of_versions: int
    shuffle_questions: bool
    shuffle_choices: bool
    change_parameters: bool
    difficulty_mode: str
    seed: int
    created_at: datetime


class VersionSummary(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    exam_id: str
    generation_config_id: int
    version_code: str
    version_index: int
    seed: int
    question_count: int
    status: str
    created_at: datetime


class GenerationResult(BaseModel):
    config: GenerationConfigOut
    versions: list[VersionSummary]
    warnings: list[str]


class IssueOut(BaseModel):
    severity: str
    code: str
    message: str
    question_id: str | None = None
    position: int | None = None


class AnswerKeyEntry(BaseModel):
    number: int  # 1-based position in this version
    question_id: str
    correct_choice_id: str
    correct_label: str


class VersionDetail(VersionSummary):
    answer_key: list[AnswerKeyEntry]
    issues: list[IssueOut]
    is_valid: bool


class RenderedChoice(BaseModel):
    label: str
    choice_id: str
    content: str


class RenderedQuestion(BaseModel):
    number: int
    question_id: str
    content: str
    image_ids: list[str]
    choices: list[RenderedChoice]
    correct_label: str


class VersionPreview(BaseModel):
    version: VersionSummary
    exam_name: str
    questions: list[RenderedQuestion]
