"""Turn ordered text/image items into draft questions. Never assume 100% accuracy:
every doubtful question carries warnings for the human review step."""
import re
from collections.abc import Sequence
from dataclasses import dataclass, field

from app.services.document import ContentItem

LABELS = "ABCDEF"


@dataclass(frozen=True)
class QuestionPattern:
    regex: re.Pattern[str]
    strict_sequence: bool = False  # only accept when number == previous + 1 (avoids false positives)


DEFAULT_QUESTION_PATTERNS: tuple[QuestionPattern, ...] = (
    # "Câu 1." / "Cau 1:" / "Question 1)" / "Q1."
    QuestionPattern(re.compile(r"^\s*(?:câu|cau|question|q)\s*(\d+)\s*[.:)\-]*\s*(.*)$", re.I)),
    # "1. ..." / "1) ..."
    QuestionPattern(re.compile(r"^\s*(\d+)\s*[.)]\s+(.*)$"), strict_sequence=True),
)

_CHOICE = re.compile(r"^\s*\(?([A-Fa-f])[.)](?:\s+(.*))?$")
_INLINE_MARK = re.compile(r"(?:^|\s)\(?([A-Fa-f])[.)](?=\s|$)\s*")
_ANSWER_LINE = re.compile(r"^\s*(?:bảng\s+)?(?:đáp\s*án(?:\s+đúng)?|dap\s*an|answer\s*key|answers?|đa)\b\s*[:\-]?\s*(.*)$", re.I)
_ANSWER_PAIR = re.compile(r"(?:câu|cau|q)?\s*(\d+)\s*[.:)\-–]?\s*([A-Fa-f])(?![A-Za-z0-9])", re.I)
_SINGLE_LETTER = re.compile(r"^\(?([A-Fa-f])\)?\.?$")
_NOISE = re.compile(r"^\s*(?:(?:trang|page)\s*\d+(?:\s*/\s*\d+)?|[-–]\s*\d+\s*[-–])\s*$", re.I)


@dataclass
class ParserConfig:
    question_patterns: Sequence[QuestionPattern] = DEFAULT_QUESTION_PATTERNS


@dataclass
class ParsedChoice:
    label: str
    content: str
    is_correct: bool = False


@dataclass
class ParsedQuestion:
    number: int
    content: str
    choices: list[ParsedChoice]
    image_indexes: list[int]
    correct_label: str | None
    warnings: list[str] = field(default_factory=list)


@dataclass
class ParseResult:
    questions: list[ParsedQuestion]
    warnings: list[str]
    preamble: list[str]


@dataclass
class _Draft:
    number: int
    lines: list[str] = field(default_factory=list)
    choices: list[list[str]] = field(default_factory=list)  # [label, content]
    images: list[int] = field(default_factory=list)
    answer: str | None = None


def _pairs(text: str) -> dict[int, str]:
    return {int(n): letter.upper() for n, letter in _ANSWER_PAIR.findall(text)}


def _is_only_pairs(text: str) -> bool:
    rest = _ANSWER_PAIR.sub("", text)
    return bool(_ANSWER_PAIR.search(text)) and not re.sub(r"[\s,;|]", "", rest)


def _split_choices(line: str, expected: int) -> list[tuple[str, str]] | None:
    """Split a line into sequential choice markers (A, B, C...). The first marker must start the line."""
    marks: list[re.Match[str]] = []
    idx = expected
    for m in _INLINE_MARK.finditer(line):
        if idx < len(LABELS) and m.group(1).upper() == LABELS[idx]:
            marks.append(m)
            idx += 1
    if not marks or line[: marks[0].start()].strip():
        return None
    out: list[tuple[str, str]] = []
    for i, m in enumerate(marks):
        end = marks[i + 1].start() if i + 1 < len(marks) else len(line)
        out.append((m.group(1).upper(), line[m.end():end].strip()))
    return out


def parse_questions(items: Sequence[ContentItem], config: ParserConfig | None = None) -> ParseResult:
    config = config or ParserConfig()
    drafts: list[_Draft] = []
    cur: _Draft | None = None
    preamble: list[str] = []
    warnings: list[str] = []
    key: dict[int, str] = {}
    in_key = False
    pending_number: int | None = None
    dropped_images = 0

    for item in items:
        if item.kind == "image":
            if cur is not None:
                cur.images.append(item.image_index)
            else:
                dropped_images += 1
            continue

        line = item.text.strip()
        if not line or _NOISE.match(line):
            continue

        if in_key:
            found = _pairs(line)
            if found:
                key.update(found)
            elif re.fullmatch(r"\d+", line):
                pending_number = int(line)
            elif pending_number is not None and (m := _SINGLE_LETTER.match(line)):
                key[pending_number] = m.group(1).upper()
                pending_number = None
            continue

        am = _ANSWER_LINE.match(line)
        if am:
            rest = am.group(1).strip()
            if not rest:
                in_key = True
                continue
            if _is_only_pairs(rest) and len(_ANSWER_PAIR.findall(rest)) >= 2:
                in_key = True
                key.update(_pairs(rest))
                continue
            single = _SINGLE_LETTER.match(rest)
            if cur is not None and single:
                cur.answer = single.group(1).upper()
                continue
        elif _is_only_pairs(line) and len(_ANSWER_PAIR.findall(line)) >= 3:
            in_key = True  # headerless answer key, e.g. "1.A 2.B 3.C 4.D"
            key.update(_pairs(line))
            continue

        started = False
        for pattern in config.question_patterns:
            m = pattern.regex.match(line)
            if not m:
                continue
            number = int(m.group(1))
            last = drafts[-1].number if drafts else 0
            if pattern.strict_sequence and number != last + 1:
                continue
            cur = _Draft(number=number)
            drafts.append(cur)
            if m.group(2).strip():
                cur.lines.append(m.group(2).strip())
            started = True
            break
        if started:
            continue

        if cur is None:
            preamble.append(line)
            continue

        pieces = _split_choices(line, len(cur.choices))
        if pieces:
            cur.choices.extend([label, text] for label, text in pieces)
        elif cur.choices:
            last_choice = cur.choices[-1]
            last_choice[1] = f"{last_choice[1]} {line}".strip()
        else:
            cur.lines.append(line)

    questions: list[ParsedQuestion] = []
    for d in drafts:
        label = d.answer or key.get(d.number)
        warns: list[str] = []
        content = " ".join(d.lines).strip()
        if not content and not d.images:
            warns.append("Không nhận diện được nội dung câu hỏi")
        if len(d.choices) < 2:
            warns.append(f"Chỉ nhận diện được {len(d.choices)} đáp án")
        for lab, text in d.choices:
            if not text:
                warns.append(f"Đáp án {lab} đang trống")
        if label is None:
            warns.append("Chưa có đáp án đúng")
        elif LABELS.index(label) >= len(d.choices):
            warns.append(f"Đáp án đúng {label} không khớp với các đáp án nhận diện được")
            label = None
        choices = [ParsedChoice(lab, text, is_correct=(lab == label)) for lab, text in d.choices]
        questions.append(ParsedQuestion(d.number, content, choices, d.images, label, warns))

    if dropped_images:
        warnings.append(f"Bỏ qua {dropped_images} hình nằm trước câu hỏi đầu tiên")
    if key and not any(q.correct_label for q in questions):
        warnings.append("Tìm thấy bảng đáp án nhưng không khớp với số câu hỏi nào")
    return ParseResult(questions, warnings, preamble)
