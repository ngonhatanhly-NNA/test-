"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import QuestionImages from "@/components/QuestionImages";
import { errorMessage } from "@/lib/api";
import { getPreview, getVersion } from "@/services/generation";
import type { VersionDetail, VersionPreview as Preview } from "@/types";

export default function VersionPreview({ versionId }: { versionId: string }) {
  const [preview, setPreview] = useState<Preview | null>(null);
  const [detail, setDetail] = useState<VersionDetail | null>(null);
  const [index, setIndex] = useState(0);
  const [showKey, setShowKey] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void (async () => {
      try {
        const [p, d] = await Promise.all([getPreview(versionId), getVersion(versionId)]);
        setPreview(p);
        setDetail(d);
      } catch (e) {
        setError(errorMessage(e));
      }
    })();
  }, [versionId]);

  if (error) return <p role="alert" className="text-sm text-red-600">{error}</p>;
  if (!preview || !detail) return <p className="text-sm text-muted">Đang tải…</p>;

  const q = preview.questions[index];
  return (
    <div className="max-w-3xl space-y-5">
      <header>
        <Link href={`/exams/${preview.version.exam_id}/generate`} className="text-sm text-brand">Danh sách mã đề</Link>
        <h1 className="text-2xl font-semibold">{preview.exam_name}, mã đề {preview.version.version_code}</h1>
        <p className="text-sm text-muted">{preview.questions.length} câu, seed {preview.version.seed}</p>
      </header>

      {!detail.is_valid && (
        <div role="alert" className="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          <p>Mã đề này chưa hợp lệ, không thể xuất:</p>
          <ul className="list-disc pl-5">
            {detail.issues.filter((i) => i.severity === "error").map((i, k) => (
              <li key={k}>{i.position ? `Câu ${i.position}: ` : ""}{i.message}</li>
            ))}
          </ul>
        </div>
      )}

      {q && (
        <article className="space-y-3 rounded border border-line bg-white p-6">
          <h2 className="font-medium">Câu {q.number}</h2>
          <p className="whitespace-pre-wrap">{q.content}</p>
          <QuestionImages ids={q.image_ids} />
          <ul className="space-y-1.5">
            {q.choices.map((c) => (
              <li key={c.label} className={`rounded px-3 py-1.5 ${showKey && c.label === q.correct_label ? "bg-green-50 text-green-800" : ""}`}>
                <span className="mr-2 font-medium">{c.label}.</span>{c.content}
              </li>
            ))}
          </ul>
        </article>
      )}

      <div className="flex items-center justify-between">
        <button onClick={() => setIndex((i) => Math.max(0, i - 1))} disabled={index === 0}
                className="rounded border border-line px-4 py-2 text-sm disabled:opacity-40">Câu trước</button>
        <span className="text-sm text-muted">{index + 1} / {preview.questions.length}</span>
        <button onClick={() => setIndex((i) => Math.min(preview.questions.length - 1, i + 1))}
                disabled={index >= preview.questions.length - 1}
                className="rounded border border-line px-4 py-2 text-sm disabled:opacity-40">Câu sau</button>
      </div>

      <section>
        <button onClick={() => setShowKey((s) => !s)} className="text-sm text-brand">
          {showKey ? "Ẩn đáp án" : "Hiện đáp án"}
        </button>
        {showKey && (
          <ol className="mt-2 grid grid-cols-5 gap-2 text-sm">
            {detail.answer_key.map((k) => (
              <li key={k.number} className="rounded border border-line bg-white px-2 py-1">
                {k.number}. <span className="font-medium">{k.correct_label}</span>
              </li>
            ))}
          </ol>
        )}
      </section>
    </div>
	
	<div className="flex items-center gap-3">
	  <a
		href={getVersionPdfDownloadUrl(version.id, false)}
		target="_blank"
		rel="noopener noreferrer"
		className="px-4 py-2 rounded-lg bg-blue-600 text-white font-medium hover:bg-blue-700 transition"
	  >
		Xuất PDF Đề thi
	  </a>
	  <a
		href={getVersionPdfDownloadUrl(version.id, true)}
		target="_blank"
		rel="noopener noreferrer"
		className="px-4 py-2 rounded-lg bg-emerald-600 text-white font-medium hover:bg-emerald-700 transition"
	  >
		Xuất PDF Đáp án
	  </a>
	</div>
  );
}
