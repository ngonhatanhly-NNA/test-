import { api } from "@/lib/api";
import type { Question, QuestionInput } from "@/types";

export const listQuestions = (examId: string) => api<Question[]>(`/api/exams/${examId}/questions`);

export const createQuestion = (examId: string, body: QuestionInput) =>
  api<Question>(`/api/exams/${examId}/questions`, { method: "POST", body: JSON.stringify(body) });

export const updateQuestion = (id: string, body: QuestionInput) =>
  api<Question>(`/api/questions/${id}`, { method: "PUT", body: JSON.stringify(body) });

export const deleteQuestion = (id: string) => api<void>(`/api/questions/${id}`, { method: "DELETE" });

export const reorderQuestions = (examId: string, ids: string[]) =>
  api<Question[]>(`/api/exams/${examId}/questions/order`, { method: "PUT", body: JSON.stringify({ question_ids: ids }) });
