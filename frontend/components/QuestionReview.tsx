"use client";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import QuestionEditor from "@/components/QuestionEditor";
import { ApiError, errorMessage } from "@/lib/api";
import { approveExam, getExam } from "@/services/exams";
import { createQuestion, deleteQuestion, listQuestions, reorderQuestions, updateQuestion } from "@/services/questions";
import type { Exam, Question, QuestionInput } from "@/types";

const STATUS: Record<string, string> = {
  DRAFT_REVIEW: "Đang kiểm tra", APPROVED: "Đã duyệt", GENERATED: "Đã tạo mã đề", EXPORTED: "Đã xuất",
};

export default function QuestionReview({ examId }: { examId: string }) {
  const [exam, setExam] = useState<Exam | null>(null);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [message, setMessage] = useState<{ kind: "error" | "ok"; text: string; list?: string[] } | null>(null);

  const reload = useCallback(async () => {
    try {
      const [e, qs] = await Promise.all([getExam(examId), listQuestions(examId)]);
      setExam(e);
      setQuestions(qs);
      setSelectedId((cur) => (cur && qs.some((q) => q.id === cur) ? cur : qs[0]?.id ?? null));
    } catch (err) {
      setMessage({ kind: "error", text: errorMessage(err) });
    }
  }, [examId]);

  useEffect(() => { void reload(); }, [reload]);

  const selected = questions.find((q) => q.id === selectedId) ?? null;

  async function onSave(id: string, body: QuestionInput) {
    await updateQuestion(id, body);
    setMessage({ kind: "ok", text: "Đã lưu câu hỏi" });
    await reload();
  }

  async function onDelete(id: string) {
    await deleteQuestion(id);
    await reload();
  }

  async function addQuestion() {
    const q = await createQuestion(examId, {
      content: "Câu hỏi mới", shuffleable: true, parameterized: false,
      choices: ["A", "B", "C", "D"].map((l) => ({ content: `Đáp án ${l}`, is_correct: false, pinned: false })),
    });
    await reload();
    setSelectedId(q.id);
  }

  async function move(index: number, delta: number) {
    const ids = questions.map((q) => q.id);
    const target = index + delta;
    if (target < 0 || target >= ids.length) return;
    [ids[index], ids[target]] = [ids[target]!, ids[index]!];
    try {
      await reorderQuestions(examId, ids);
      await reload();
    } catch (e) {
      setMessage({ kind: "error", text: errorMessage(e) });
    }
  }

  async function approve() {
    try {
      await approveExam(examId);
      setMessage({ kind: "ok", text: "Đã duyệt đề. Bạn có thể tạo mã đề." });
      await reload();
    } catch (e) {
      const problems = e instanceof ApiError ? (e.details.problems as { question_id: string; message: string }[] | undefined) : undefined;
      setMessage({
        kind: "error", text: errorMessage(e),
        list: problems?.map((p) => `${p.question_id ? `Câu ${questions.findIndex((q) => q.id === p.question_id) + 1}: ` : ""}${p.message}`),
      });
    }
  }

  if (!exam) return <p className="text-sm text-muted">{message?.text ?? "Đang tải…"}</p>;

  return (
    <div className="space-y-5">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <Link href="/" className="text-sm text-brand">Trang chủ</Link>
          <h1 className="text-2xl font-semibold">{exam.name}</h1>
          <p className="text-sm text-muted">
            {questions.length} câu hỏi, {exam.version_count} mã đề, {STATUS[exam.status] ?? exam.status}
          </p>
        </div>
        <div className="flex gap-2">
          <button onClick={() => void approve()} className="rounded border border-brand px-4 py-2 text-sm text-brand">
            Duyệt đề
          </button>
          <Link href={`/exams/${examId}/generate`} className="rounded bg-brand px-4 py-2 text-sm font-medium text-white">
            Tạo mã đề
          </Link>
        </div>
      </header>

      {message && (
        <div role={message.kind === "error" ? "alert" : "status"}
             className={`rounded border px-3 py-2 text-sm ${message.kind === "error" ? "border-red-200 bg-red-50 text-red-700" : "border-green-200 bg-green-50 text-green-700"}`}>
          <p>{message.text}</p>
          {message.list && <ul className="mt-1 list-disc pl-5">{message.list.map((l) => <li key={l}>{l}</li>)}</ul>}
        </div>
      )}

      <div className="grid grid-cols-[14rem_1fr] gap-5">
        <aside aria-label="Danh sách câu hỏi" className="space-y-2">
          <ul className="space-y-1">
            {questions.map((q, i) => (
              <li key={q.id} className="flex items-center gap-1">
                <button onClick={() => setSelectedId(q.id)} aria-current={q.id === selectedId}
                        className={`flex flex-1 items-center justify-between rounded px-3 py-1.5 text-left text-sm ${q.id === selectedId ? "bg-brand-soft font-medium text-brand" : "bg-white hover:bg-brand-soft"}`}>
                  <span>Câu {i + 1}</span>
                  {(q.import_warnings.length > 0 || !q.correct_choice_id) && (
                    <span className="h-2 w-2 rounded-full bg-amber-500" title="Cần kiểm tra" />
                  )}
                </button>
                <button onClick={() => void move(i, -1)} disabled={i === 0} aria-label="Chuyển lên" className="px-1 text-muted disabled:opacity-30">↑</button>
                <button onClick={() => void move(i, 1)} disabled={i === questions.length - 1} aria-label="Chuyển xuống" className="px-1 text-muted disabled:opacity-30">↓</button>
              </li>
            ))}
          </ul>
          <button onClick={() => void addQuestion()} className="w-full rounded border border-dashed border-line px-3 py-1.5 text-sm text-brand">
            Thêm câu hỏi
          </button>
        </aside>

        <div>
          {selected ? (
            <QuestionEditor key={`${selected.id}-${selected.choices.map((c) => c.id).join()}`} question={selected} onSave={onSave} onDelete={onDelete} />
          ) : (
            <p className="text-sm text-muted">Đề thi chưa có câu hỏi. Chọn “Thêm câu hỏi” để bắt đầu.</p>
          )}
        </div>
      </div>
    </div>
  );
}
