from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, field_validator


class ExamCreate(BaseModel):
    name: str = Field(min_length=1, max_length=255)
    description: str | None = None
    subject: str | None = Field(default=None, max_length=64)

    @field_validator("name")
    @classmethod
    def _name(cls, v: str) -> str:
        v = v.strip()
        if not v:
            raise ValueError("Tên đề thi không được để trống")
        return v


class ExamUpdate(BaseModel):
    name: str | None = Field(default=None, min_length=1, max_length=255)
    description: str | None = None
    subject: str | None = Field(default=None, max_length=64)


class ExamOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    name: str
    description: str | None
    subject: str | None
    status: str
    question_count: int
    version_count: int
    created_at: datetime
    updated_at: datetime


class UploadWarning(BaseModel):
    question_id: str | None
    number: int | None
    message: str


class UploadResult(BaseModel):
    exam: ExamOut
    source: str  # "text" | "ocr"
    question_count: int
    warnings: list[UploadWarning]
