"use client";
import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import { errorMessage } from "@/lib/api";
import { getExam } from "@/services/exams";
import { generateVersions, listVersions } from "@/services/generation";
import type { Exam, GenerationResult, VersionSummary } from "@/types";

export default function GenerateForm({ examId }: { examId: string }) {
  const [exam, setExam] = useState<Exam | null>(null);
  const [versions, setVersions] = useState<VersionSummary[]>([]);
  const [count, setCount] = useState(10);
  const [shuffleQuestions, setShuffleQuestions] = useState(true);
  const [shuffleChoices, setShuffleChoices] = useState(true);
  const [seed, setSeed] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<GenerationResult | null>(null);

  useEffect(() => {
    void (async () => {
      try {
        setExam(await getExam(examId));
        setVersions(await listVersions(examId));
      } catch (e) {
        setError(errorMessage(e));
      }
    })();
  }, [examId]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!Number.isInteger(count) || count < 1 || count > 50) return setError("Số lượng mã đề từ 1 đến 50");
    if (seed.trim() !== "" && !/^\d+$/.test(seed.trim())) return setError("Seed phải là số nguyên không âm");
    setBusy(true);
    setError(null);
    try {
      const r = await generateVersions(examId, {
        number_of_versions: count, shuffle_questions: shuffleQuestions, shuffle_choices: shuffleChoices,
        change_parameters: false, difficulty_mode: "same", seed: seed.trim() === "" ? null : Number(seed),
      });
      setResult(r);
      setSeed(String(r.config.seed));
      setVersions(await listVersions(examId));
      setExam(await getExam(examId));
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const ready = exam && ["APPROVED", "GENERATED", "EXPORTED"].includes(exam.status);

  return (
    <div className="max-w-3xl space-y-6">
      <header>
        <Link href={`/exams/${examId}`} className="text-sm text-brand">Quay lại kiểm tra câu hỏi</Link>
        <h1 className="text-2xl font-semibold">Tạo mã đề{exam ? `: ${exam.name}` : ""}</h1>
      </header>

      {exam && !ready && (
        <p role="alert" className="rounded border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          Đề thi chưa được duyệt. Hãy kiểm tra câu hỏi và chọn “Duyệt đề” trước khi tạo mã đề.
        </p>
      )}

      <form onSubmit={onSubmit} className="space-y-4 rounded border border-line bg-white p-5">
        <label className="block text-sm font-medium">
          Số lượng mã đề
          <input type="number" min={1} max={50} value={count} onChange={(e) => setCount(Number(e.target.value))}
                 className="mt-1 block w-28 rounded border border-line px-3 py-1.5 font-normal" />
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={shuffleQuestions} onChange={(e) => setShuffleQuestions(e.target.checked)} />
          Trộn câu hỏi
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={shuffleChoices} onChange={(e) => setShuffleChoices(e.target.checked)} />
          Trộn đáp án
        </label>
        <label className="flex items-center gap-2 text-sm text-muted">
          <input type="checkbox" disabled /> Thay đổi số liệu (sắp có)
        </label>
        <label className="block text-sm font-medium">
          Seed (để trống để tự chọn; dùng lại cùng seed sẽ ra cùng thứ tự)
          <input value={seed} onChange={(e) => setSeed(e.target.value)} inputMode="numeric"
                 className="mt-1 block w-40 rounded border border-line px-3 py-1.5 font-normal" />
        </label>
        {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
        <button disabled={busy || !ready} className="rounded bg-brand px-4 py-2 text-sm font-medium text-white disabled:opacity-60">
          {busy ? "Đang tạo…" : "Tạo mã đề"}
        </button>
      </form>

      {result && (
        <div role="status" className="space-y-1 text-sm text-green-700">
          <p>Đã tạo {result.versions.length} mã đề (seed {result.config.seed}).</p>
          {result.warnings.map((w) => <p key={w} className="text-amber-700">{w}</p>)}
        </div>
      )}

      {versions.length > 0 && (
        <section>
          <h2 className="mb-2 text-lg font-medium">Các mã đề</h2>
          <table className="w-full overflow-hidden rounded border border-line bg-white text-sm">
            <thead className="bg-paper text-left text-muted">
              <tr><th className="px-4 py-2">Mã đề</th><th className="px-4 py-2">Số câu</th><th className="px-4 py-2">Seed</th><th className="px-4 py-2" /></tr>
            </thead>
            <tbody>
              {versions.map((v) => (
                <tr key={v.id} className="border-t border-line">
                  <td className="px-4 py-2 font-medium">{v.version_code}</td>
                  <td className="px-4 py-2">{v.question_count}</td>
                  <td className="px-4 py-2">{v.seed}</td>
                  <td className="px-4 py-2 text-right"><Link href={`/versions/${v.id}`} className="text-brand">Xem trước</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
    </div>
  );
}
