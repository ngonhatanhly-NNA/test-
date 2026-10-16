"use client";
import { useState } from "react";
import QuestionImages from "@/components/QuestionImages";
import { errorMessage } from "@/lib/api";
import type { ChoiceInput, Question, QuestionInput } from "@/types";

const LABELS = "ABCDEF";
const MAX_CHOICES = 6;

interface Props {
  question: Question;
  onSave: (id: string, body: QuestionInput) => Promise<void>;
  onDelete: (id: string) => Promise<void>;
}

export default function QuestionEditor({ question, onSave, onDelete }: Props) {
  const [content, setContent] = useState(question.content);
  const [choices, setChoices] = useState<ChoiceInput[]>(
    question.choices.map((c) => ({ id: c.id, content: c.content, is_correct: c.is_correct, pinned: c.pinned })),
  );
  const [shuffleable, setShuffleable] = useState(question.shuffleable);
  const [parameterized, setParameterized] = useState(question.parameterized);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const setChoice = (i: number, patch: Partial<ChoiceInput>) =>
    setChoices((cs) => cs.map((c, k) => (k === i ? { ...c, ...patch } : c)));
  const setCorrect = (i: number) => setChoices((cs) => cs.map((c, k) => ({ ...c, is_correct: k === i })));

  async function save() {
    if (!content.trim()) return setError("Nhập nội dung câu hỏi");
    if (choices.length < 2) return setError("Cần ít nhất 2 đáp án");
    if (choices.some((c) => !c.content.trim())) return setError("Đáp án không được để trống");
    setSaving(true);
    try {
      await onSave(question.id, { content, shuffleable, parameterized, choices });
      setError(null);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="space-y-4 rounded border border-line bg-white p-5" aria-label={`Câu hỏi ${question.id}`}>
      <header className="flex items-baseline justify-between">
        <h2 className="text-lg font-medium">Câu {question.position + 1}</h2>
        <span className="text-xs text-muted">{question.id}</span>
      </header>

      {question.import_warnings.length > 0 && (
        <ul className="rounded border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {question.import_warnings.map((w) => <li key={w}>{w}</li>)}
        </ul>
      )}

      <QuestionImages ids={question.image_ids} />

      <label className="block text-sm font-medium">
        Nội dung
        <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={3}
                  className="mt-1 w-full rounded border border-line px-3 py-2 text-sm font-normal" />
      </label>

      <fieldset className="space-y-2">
        <legend className="text-sm font-medium">Đáp án (chọn nút tròn cho đáp án đúng)</legend>
        {choices.map((c, i) => (
          <div key={c.id ?? `new-${i}`} className="flex items-center gap-2">
            <input type="radio" name={`correct-${question.id}`} checked={c.is_correct} onChange={() => setCorrect(i)}
                   aria-label={`Đáp án ${LABELS[i]} là đáp án đúng`} />
            <span className="w-5 text-sm font-medium">{LABELS[i]}.</span>
            <input value={c.content} onChange={(e) => setChoice(i, { content: e.target.value })}
                   aria-label={`Nội dung đáp án ${LABELS[i]}`}
                   className="flex-1 rounded border border-line px-3 py-1.5 text-sm" />
            <label className="flex items-center gap-1 text-xs text-muted" title="Giữ nguyên vị trí khi trộn đáp án">
              <input type="checkbox" checked={c.pinned} onChange={(e) => setChoice(i, { pinned: e.target.checked })} />
              Cố định
            </label>
            <button type="button" className="text-sm text-muted hover:text-red-600"
                    onClick={() => setChoices((cs) => cs.filter((_, k) => k !== i))}
                    aria-label={`Xóa đáp án ${LABELS[i]}`}>Xóa</button>
          </div>
        ))}
        {choices.length < MAX_CHOICES && (
          <button type="button" className="text-sm text-brand"
                  onClick={() => setChoices((cs) => [...cs, { content: "", is_correct: false, pinned: false }])}>
            Thêm đáp án
          </button>
        )}
      </fieldset>

      <div className="flex gap-6 text-sm">
        <label className="flex items-center gap-2">
          <input type="checkbox" checked={shuffleable} onChange={(e) => setShuffleable(e.target.checked)} />
          Cho phép trộn câu này
        </label>
        <label className="flex items-center gap-2">
          <input type="checkbox" checked={parameterized} onChange={(e) => setParameterized(e.target.checked)} />
          Câu hỏi có số liệu thay đổi
        </label>
      </div>

      {error && <p role="alert" className="text-sm text-red-600">{error}</p>}

      <div className="flex gap-2">
        <button onClick={() => void save()} disabled={saving}
                className="rounded bg-brand px-4 py-2 text-sm font-medium text-white disabled:opacity-60">
          {saving ? "Đang lưu…" : "Lưu câu hỏi"}
        </button>
        <button onClick={() => { if (window.confirm("Xóa câu hỏi này?")) void onDelete(question.id); }}
                className="rounded border border-line px-4 py-2 text-sm text-red-600">
          Xóa câu hỏi
        </button>
      </div>
    </section>
  );
}
