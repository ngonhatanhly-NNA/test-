import re
import unicodedata
from pathlib import PurePosixPath, PureWindowsPath

from app.core.errors import AppError


def sanitize_filename(filename: str | None, default: str = "upload.pdf") -> str:
    """Strip directories and unsafe characters; the result is only used for display/metadata."""
    name = PureWindowsPath(PurePosixPath(filename or "").name).name
    name = unicodedata.normalize("NFC", name)
    name = re.sub(r"[^\w\s.\-]", "_", name, flags=re.UNICODE)
    name = re.sub(r"\s+", " ", name).strip(" .")
    if len(name) > 100:
        stem, dot, ext = name.rpartition(".")
        name = (stem[: 100 - len(ext) - 1] + dot + ext) if dot else name[:100]
    return name or default


def validate_pdf_upload(filename: str | None, data: bytes, max_size: int) -> str:
    """Validate extension, size and magic bytes. Returns the sanitized filename."""
    safe = sanitize_filename(filename)
    if not safe.lower().endswith(".pdf"):
        raise AppError("INVALID_FILE_TYPE", "Chỉ hỗ trợ file PDF", 415)
    if not data:
        raise AppError("EMPTY_FILE", "File rỗng", 422)
    if len(data) > max_size:
        raise AppError(
            "FILE_TOO_LARGE", f"File vượt quá dung lượng tối đa {max_size // (1024 * 1024)} MB", 413
        )
    if not data.lstrip()[:5] == b"%PDF-":
        raise AppError("INVALID_FILE_TYPE", "Nội dung file không phải PDF hợp lệ", 415)
    return safe
