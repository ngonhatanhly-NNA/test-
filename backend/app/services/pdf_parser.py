"""PDF -> ordered text lines and images (reading order). No question logic here."""
import logging

import pymupdf

from app.core.errors import AppError
from app.services.document import ContentItem, ExtractedDocument, ExtractedImage
from app.services.ocr_service import OCRService

logger = logging.getLogger("app.pdf")

MIN_IMAGE_SIDE = 24
MIN_TEXT_CHARS_PER_PAGE = 40
MAX_PAGES = 200
OCR_DPI = 200


def open_pdf(data: bytes) -> pymupdf.Document:
    try:
        doc = pymupdf.open(stream=data, filetype="pdf")
    except Exception as exc:
        raise AppError("PDF_INVALID", "Không đọc được file PDF", 422) from exc
    if doc.needs_pass:
        doc.close()
        raise AppError("PDF_ENCRYPTED", "PDF được bảo vệ bằng mật khẩu", 422)
    if doc.page_count == 0:
        doc.close()
        raise AppError("PDF_EMPTY", "PDF không có trang nào", 422)
    if doc.page_count > MAX_PAGES:
        doc.close()
        raise AppError("PDF_TOO_LONG", f"PDF vượt quá {MAX_PAGES} trang", 422)
    return doc


def has_text_layer(doc: pymupdf.Document) -> bool:
    total = sum(len(page.get_text("text").strip()) for page in doc)
    return total / doc.page_count >= MIN_TEXT_CHARS_PER_PAGE


def _normalize_image(data: bytes, ext: str) -> tuple[bytes, str]:
    if ext in ("png", "jpeg", "jpg"):
        return data, "jpeg" if ext == "jpg" else ext
    pix = pymupdf.Pixmap(data)
    if pix.n - pix.alpha > 3:
        pix = pymupdf.Pixmap(pymupdf.csRGB, pix)
    return pix.tobytes("png"), "png"


def extract_text_document(doc: pymupdf.Document) -> ExtractedDocument:
    items: list[ContentItem] = []
    images: list[ExtractedImage] = []
    for page_no, page in enumerate(doc):
        for block in page.get_text("dict", sort=True)["blocks"]:
            if block["type"] == 0:
                for line in block["lines"]:
                    text = "".join(span["text"] for span in line["spans"]).strip()
                    if text:
                        items.append(ContentItem("text", text=text, page=page_no))
            elif block["type"] == 1:
                if min(block["width"], block["height"]) < MIN_IMAGE_SIDE:
                    continue
                try:
                    data, ext = _normalize_image(block["image"], block["ext"])
                except Exception:
                    logger.warning("skip unreadable image on page %s", page_no + 1)
                    continue
                images.append(ExtractedImage(data, ext, block["width"], block["height"], page_no))
                items.append(ContentItem("image", image_index=len(images) - 1, page=page_no))
    return ExtractedDocument(items, images, doc.page_count, "text")


def extract_ocr_document(doc: pymupdf.Document, ocr: OCRService) -> ExtractedDocument:
    items: list[ContentItem] = []
    for page_no, page in enumerate(doc):
        png = page.get_pixmap(dpi=OCR_DPI).tobytes("png")
        for line in ocr.extract_text(png).splitlines():
            if line.strip():
                items.append(ContentItem("text", text=line.strip(), page=page_no))
    return ExtractedDocument(items, [], doc.page_count, "ocr")


def extract_document(data: bytes, ocr: OCRService) -> ExtractedDocument:
    doc = open_pdf(data)
    try:
        if has_text_layer(doc):
            logger.info("pdf has text layer pages=%s", doc.page_count)
            return extract_text_document(doc)
        logger.info("pdf looks scanned, using OCR pages=%s", doc.page_count)
        return extract_ocr_document(doc, ocr)
    finally:
        doc.close()
