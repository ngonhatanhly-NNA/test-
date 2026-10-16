from dataclasses import dataclass
from typing import Literal


@dataclass
class ExtractedImage:
    data: bytes
    ext: str  # "png" | "jpeg"
    width: int
    height: int
    page: int


@dataclass
class ContentItem:
    kind: Literal["text", "image"]
    text: str = ""
    image_index: int = -1
    page: int = 0


@dataclass
class ExtractedDocument:
    items: list[ContentItem]
    images: list[ExtractedImage]
    page_count: int
    source: Literal["text", "ocr"]
