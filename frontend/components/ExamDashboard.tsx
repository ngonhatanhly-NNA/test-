"use client";
import Link from "next/link";
import { ChangeEvent, FormEvent, useRef, useState } from "react";
import { useExams } from "@/hooks/useExams";
import { errorMessage } from "@/lib/api";
import { createExam, deleteExam, uploadExam } from "@/services/exams";

const STATUS_LABEL: Record<string, string> = {
  DRAFT_REVIEW: "Đang kiểm tra",
  APPROVED: "Đã duyệt",
  GENERATING: "Đang tạo mã đề",
  GENERATED: "Đã tạo mã đề",
  EXPORTED: "Đã xuất",
};

export default function ExamDashboard() {
  const { exams, loading, error, reload } = useExams();
  const [name, setName] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);

  async function onFile(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setUploading(true);
    setFormError(null);
    setNotice(null);
    try {
      const r = await uploadExam(file);
      setNotice(`Đã phân tích ${r.question_count} câu hỏi. ${r.warnings.length > 0 ? `${r.warnings.length} mục cần kiểm tra.` : ""}`);
      await reload();
    } catch (err) {
      setFormError(errorMessage(err));
    } finally {
      setUploading(false);
    }
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) return setFormError("Nhập tên đề thi");
    try {
      await createExam(name.trim());
      setName("");
      setFormError(null);
      await reload();
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "Có lỗi xảy ra");
    }
  }

  async function onDelete(id: string) {
    if (!window.confirm("Xóa đề thi này và toàn bộ câu hỏi?")) return;
    await deleteExam(id);
    await reload();
  }

  return (
    <div className="space-y-8">
      <h1 className="text-2xl font-semibold">Trang chủ</h1>

      <form onSubmit={onSubmit} className="flex max-w-xl gap-2">
        <label className="sr-only" htmlFor="exam-name">Tên đề thi</label>
        <input
          id="exam-name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Tên đề thi, ví dụ: Vật lý giữa kỳ"
          className="flex-1 rounded border border-line bg-white px-3 py-2 text-sm"
        />
        <button className="rounded bg-brand px-4 py-2 text-sm font-medium text-white hover:opacity-90">
          Tạo đề thi
        </button>
      </form>
      <div>
        <input ref={fileInput} type="file" accept="application/pdf,.pdf" onChange={(e) => void onFile(e)} className="hidden"
               aria-label="Chọn file PDF" />
        <button onClick={() => fileInput.current?.click()} disabled={uploading}
                className="rounded border border-brand px-4 py-2 text-sm text-brand disabled:opacity-60">
          {uploading ? "Đang phân tích…" : "Tải đề PDF"}
        </button>
      </div>
      {notice && <p role="status" className="text-sm text-green-700">{notice}</p>}
      {formError && <p role="alert" className="text-sm text-red-600">{formError}</p>}

      <section>
        <h2 className="mb-3 text-lg font-medium">Đề thi gần đây</h2>
        {loading && <p className="text-sm text-muted">Đang tải…</p>}
        {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
        {!loading && !error && exams.length === 0 && (
          <p className="text-sm text-muted">Chưa có đề thi nào. Nhập tên ở trên để tạo đề đầu tiên.</p>
        )}
        <ul className="divide-y divide-line rounded border border-line bg-white">
          {exams.map((x) => (
            <li key={x.id} className="flex items-center justify-between px-4 py-3">
              <div>
                <Link href={`/exams/${x.id}`} className="font-medium text-brand hover:underline">{x.name}</Link>
                <p className="text-sm text-muted">
                  {x.question_count} câu hỏi, {x.version_count} mã đề, {STATUS_LABEL[x.status] ?? x.status}
                </p>
              </div>
              <button onClick={() => void onDelete(x.id)} className="text-sm text-muted hover:text-red-600">
                Xóa
              </button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
