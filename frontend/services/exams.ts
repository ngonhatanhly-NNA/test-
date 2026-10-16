import { api } from "@/lib/api";
import type { Exam, UploadResult } from "@/types";

export const listExams = () => api<Exam[]>("/api/exams");
export const getExam = (id: string) => api<Exam>(`/api/exams/${id}`);

export const createExam = (name: string, subject?: string) =>
  api<Exam>("/api/exams", { method: "POST", body: JSON.stringify({ name, subject: subject || null }) });

export const deleteExam = (id: string) => api<void>(`/api/exams/${id}`, { method: "DELETE" });
export const approveExam = (id: string) => api<Exam>(`/api/exams/${id}/approve`, { method: "POST" });

export function uploadExam(file: File) {
  const form = new FormData();
  form.append("file", file);
  return api<UploadResult>("/api/exams/upload", { method: "POST", body: form });
}
