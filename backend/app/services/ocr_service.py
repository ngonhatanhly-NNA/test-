from abc import ABC, abstractmethod

from app.core.errors import AppError


class OCRService(ABC):
    """Abstraction so Tesseract can later be swapped for another OCR API."""

    @abstractmethod
    def extract_text(self, image_png: bytes) -> str: ...


class NullOCRService(OCRService):
    def extract_text(self, image_png: bytes) -> str:
        raise AppError(
            "OCR_NOT_AVAILABLE",
            "PDF này là bản scan (không có lớp chữ) và OCR chưa được bật. Hãy dùng PDF có thể chọn/copy chữ.",
            422,
        )


def get_ocr_service() -> OCRService:
    return NullOCRService()  # Phase 7 returns a real implementation when OCR_ENABLED=true
