# backend/app/services/pdf_generator.py
import io
import os
from typing import Any, Dict, List
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    Image,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)

FONT_PATH = os.getenv(
    "PDF_FONT_PATH", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
)
FONT_BOLD_PATH = os.getenv(
    "PDF_FONT_BOLD_PATH", "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
)


def _register_vietnamese_fonts() -> Tuple[str, str]:
    if os.path.exists(FONT_PATH) and os.path.exists(FONT_BOLD_PATH):
        pdfmetrics.registerFont(TTFont("VNFont", FONT_PATH))
        pdfmetrics.registerFont(TTFont("VNFont-Bold", FONT_BOLD_PATH))
        return "VNFont", "VNFont-Bold"
    return "Helvetica", "Helvetica-Bold"


class PDFGeneratorService:
    @staticmethod
    def build_exam_version_pdf(
        exam_title: str,
        version_code: str,
        questions: List[Dict[str, Any]],
    ) -> bytes:
        """
        Tạo file PDF cho 1 mã đề thi.
        questions: Danh sách câu hỏi đã trộn thứ tự và trộn đáp án A/B/C/D.
        """
        buffer = io.BytesIO()
        font_regular, font_bold = _register_vietnamese_fonts()

        doc = SimpleDocTemplate(
            buffer,
            pagesize=A4,
            leftMargin=20 * mm,
            rightMargin=20 * mm,
            topMargin=18 * mm,
            bottomMargin=18 * mm,
        )

        styles = getSampleStyleSheet()
        title_style = ParagraphStyle(
            "ExamTitle",
            parent=styles["Heading1"],
            fontName=font_bold,
            fontSize=14,
            alignment=1,
            spaceAfter=6,
        )
        code_style = ParagraphStyle(
            "VersionCode",
            parent=styles["Normal"],
            fontName=font_bold,
            fontSize=12,
            alignment=1,
            spaceAfter=14,
        )
        question_style = ParagraphStyle(
            "QuestionText",
            parent=styles["Normal"],
            fontName=font_regular,
            fontSize=11,
            leading=15,
            spaceBefore=8,
            spaceAfter=4,
        )
        choice_style = ParagraphStyle(
            "ChoiceText",
            parent=styles["Normal"],
            fontName=font_regular,
            fontSize=10.5,
            leading=14,
            leftIndent=12,
            spaceAfter=2,
        )

        story = [
            Paragraph(exam_title.upper(), title_style),
            Paragraph(f"MÃ ĐỀ: {version_code}", code_style),
            Spacer(1, 4 * mm),
        ]

        labels = ["A", "B", "C", "D", "E", "F"]
        for idx, q in enumerate(questions, start=1):
            q_text = f"<b>Câu {idx}:</b> {q['content']}"
            story.append(Paragraph(q_text, question_style))

            # Hiển thị hình ảnh đi kèm câu hỏi (nếu có từ QuestionImage)
            for img_path in q.get("images", []):
                if os.path.exists(img_path):
                    story.append(Spacer(1, 2 * mm))
                    story.append(Image(img_path, width=80 * mm, height=45 * mm, kind="proportional"))
                    story.append(Spacer(1, 2 * mm))

            for c_idx, choice in enumerate(q.get("choices", [])):
                label = choice.get("label") or labels[c_idx]
                c_text = f"<b>{label}.</b> {choice['content']}"
                story.append(Paragraph(c_text, choice_style))

        doc.build(story)
        buffer.seek(0)
        return buffer.read()

    @staticmethod
    def build_answer_key_pdf(
        exam_title: str,
        version_code: str,
        answer_mappings: List[Dict[str, Any]],
    ) -> bytes:
        """
        Tạo file PDF bảng đáp án cho mã đề.
        answer_mappings: [{"question_number": 1, "correct_label": "B"}, ...]
        """
        buffer = io.BytesIO()
        font_regular, font_bold = _register_vietnamese_fonts()

        doc = SimpleDocTemplate(buffer, pagesize=A4)
        styles = getSampleStyleSheet()
        title_style = ParagraphStyle(
            "KeyTitle",
            parent=styles["Heading1"],
            fontName=font_bold,
            fontSize=14,
            alignment=1,
            spaceAfter=12,
        )

        story = [
            Paragraph(f"ĐÁP ÁN — {exam_title.upper()} (MÃ ĐỀ: {version_code})", title_style),
            Spacer(1, 6 * mm),
        ]

        data = [["Câu", "Đáp án đúng"]]
        for item in sorted(answer_mappings, key=lambda x: x["question_number"]):
            data.append([str(item["question_number"]), item["correct_label"]])

        table = Table(data, colWidths=[35 * mm, 45 * mm])
        table.setStyle(
            TableStyle(
                [
                    ("FONTNAME", (0, 0), (-1, 0), font_bold),
                    ("FONTNAME", (0, 1), (-1, -1), font_regular),
                    ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#E2E8F0")),
                    ("ALIGN", (0, 0), (-1, -1), "CENTER"),
                    ("GRID", (0, 0), (-1, -1), 0.5, colors.grey),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
                    ("TOPPADDING", (0, 0), (-1, -1), 6),
                ]
            )
        )
        story.append(table)
        doc.build(story)
        buffer.seek(0)
        return buffer.read()