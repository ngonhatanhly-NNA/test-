# Smart Exam Generator

Phase 1-4: setup, CRUD, import PDF (parse + review), tạo mã đề (trộn câu hỏi/đáp án, đáp án theo mapping).

## Chạy bằng Docker
    cp .env.example .env
    docker compose up --build
Frontend http://localhost:3000, API docs http://localhost:8000/docs

## Chạy backend không cần Docker
    cd backend && pip install -r requirements.txt
    alembic upgrade head && uvicorn app.main:app --reload
    pytest

## Luồng làm việc
UPLOADED → PARSING → DRAFT_REVIEW → (duyệt) APPROVED → GENERATING → GENERATED.
Sửa câu hỏi sau khi duyệt sẽ đưa đề về DRAFT_REVIEW; các mã đề đã tạo không đổi (mỗi mã đề dùng snapshot câu hỏi).

## Quy ước
- ID bất biến, không tái sử dụng: E (đề), Q (câu), C (đáp án), I (hình), V (mã đề), QV (snapshot câu hỏi).
- Đáp án đúng chỉ lưu bằng `Choice.is_correct` (suy ra `correct_choice_id`), không bao giờ bằng chữ A/B/C/D.
- Mỗi mã đề lưu: thứ tự câu, `label -> choice_id` của từng câu, seed. Đáp án tính từ mapping cuối cùng.
- RNG của mã đề = f(seed, version_index) nên tạo lại cùng seed ra cùng kết quả.
- `shuffleable=false`: giữ nguyên vị trí và thứ tự đáp án của câu đó. `pinned`: đáp án giữ vị trí khi trộn đáp án.
- Parser: mẫu nhận diện cấu hình ở `app/services/question_parser.py` (`ParserConfig`).
