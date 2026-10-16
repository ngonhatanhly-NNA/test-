from app.services.document import ContentItem
from app.services.question_parser import parse_questions


def T(*lines):
    return [ContentItem("text", text=line) for line in lines]


def test_cau_format():
    r = parse_questions(T("ĐỀ KIỂM TRA", "Câu 1. Thủ đô của Việt Nam là gì?", "A. Huế", "B. Hà Nội", "C. Đà Nẵng", "D. Cần Thơ",
                          "Câu 2. 2 + 2 bằng?", "A. 3", "B. 4", "C. 5", "D. 6"))
    assert r.preamble == ["ĐỀ KIỂM TRA"]
    assert [q.number for q in r.questions] == [1, 2]
    q1 = r.questions[0]
    assert q1.content == "Thủ đô của Việt Nam là gì?"
    assert [c.content for c in q1.choices] == ["Huế", "Hà Nội", "Đà Nẵng", "Cần Thơ"]
    assert q1.correct_label is None and "Chưa có đáp án đúng" in q1.warnings


def test_question_with_paren_choices():
    r = parse_questions(T("Question 1) What is 1+1?", "A) 1", "B) 2", "C) 3", "D) 4"))
    assert [c.label for c in r.questions[0].choices] == ["A", "B", "C", "D"]


def test_numbered_format_requires_sequence():
    r = parse_questions(T("1. First?", "A. a", "B. b", "2. Second?", "A. a", "B. b", "7. not a new question", "C. c"))
    assert [q.number for q in r.questions] == [1, 2]
    # not a new question: it continues the last open choice
    assert r.questions[1].choices[1].content == "b 7. not a new question"


def test_multiline_content_and_choices():
    r = parse_questions(T("Câu 1. Một vật chuyển động", "với vận tốc 5 m/s?", "A. 10 m", "kèm đơn vị", "B. 20 m", "C. 30 m", "D. 40 m"))
    q = r.questions[0]
    assert q.content == "Một vật chuyển động với vận tốc 5 m/s?"
    assert q.choices[0].content == "10 m kèm đơn vị"


def test_inline_choices_on_one_line():
    r = parse_questions(T("Câu 1. Tính?", "A. 10 B. 20 C. 30 D. 40"))
    assert [c.content for c in r.questions[0].choices] == ["10", "20", "30", "40"]


def test_inline_answer_line():
    r = parse_questions(T("Câu 1. Q?", "A. a", "B. b", "C. c", "D. d", "Đáp án: C"))
    q = r.questions[0]
    assert q.correct_label == "C" and [c.is_correct for c in q.choices] == [False, False, True, False]


def test_answer_key_section_with_header():
    r = parse_questions(T("Câu 1. Q1?", "A. a", "B. b", "Câu 2. Q2?", "A. a", "B. b", "ĐÁP ÁN", "1. B", "2. A"))
    assert [q.correct_label for q in r.questions] == ["B", "A"]


def test_answer_key_on_one_line_and_headerless():
    r1 = parse_questions(T("Câu 1. Q?", "A. a", "B. b", "Câu 2. Q?", "A. a", "B. b", "Đáp án: 1-B 2-A"))
    assert [q.correct_label for q in r1.questions] == ["B", "A"]
    r2 = parse_questions(T("Câu 1. Q?", "A. a", "B. b", "Câu 2. Q?", "A. a", "B. b", "Câu 3. Q?", "A. a", "B. b",
                           "1.A 2.B 3.A"))
    assert [q.correct_label for q in r2.questions] == ["A", "B", "A"]


def test_answer_key_split_across_lines():
    r = parse_questions(T("Câu 1. Q?", "A. a", "B. b", "Câu 2. Q?", "A. a", "B. b", "ĐÁP ÁN", "1", "B", "2", "A"))
    assert [q.correct_label for q in r.questions] == ["B", "A"]


def test_dap_an_inside_question_text_is_not_an_answer():
    r = parse_questions(T("Câu 1. Chọn", "đáp án nào đúng?", "A. a", "B. b"))
    assert r.questions[0].content == "Chọn đáp án nào đúng?"


def test_warnings_for_bad_questions():
    r = parse_questions(T("Câu 1. Chỉ có một đáp án", "A. a", "Câu 2. Đáp án đúng không tồn tại", "A. a", "B. b", "Đáp án: D"))
    assert "Chỉ nhận diện được 1 đáp án" in r.questions[0].warnings
    assert any("không khớp" in w for w in r.questions[1].warnings)
    assert r.questions[1].correct_label is None


def test_images_attach_to_current_question_and_page_noise_ignored():
    items = T("Câu 1. Cho hình vẽ:") + [ContentItem("image", image_index=0)] + T("Trang 1/3", "A. a", "B. b")
    r = parse_questions([ContentItem("image", image_index=9)] + items)
    assert r.questions[0].image_indexes == [0]
    assert len(r.warnings) == 1 and r.questions[0].choices[0].content == "a"


def test_no_questions():
    assert parse_questions(T("Just some text", "nothing here")).questions == []
