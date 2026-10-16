import { api } from "@/lib/api";
import type { GenerationConfigInput, GenerationResult, VersionDetail, VersionPreview, VersionSummary } from "@/types";
// Bổ sung vào frontend/services/generation.ts
import { API_BASE_URL } from "@/lib/api";

export function getVersionPdfDownloadUrl(versionId: number | string, includeAnswers = false): string {
  return `${API_BASE_URL}/api/versions/${versionId}/export-pdf?include_answers=${includeAnswers}`;
}
export const generateVersions = (examId: string, config: GenerationConfigInput) =>
  api<GenerationResult>(`/api/exams/${examId}/generate`, { method: "POST", body: JSON.stringify(config) });

export const listVersions = (examId: string) => api<VersionSummary[]>(`/api/exams/${examId}/versions`);
export const getVersion = (id: string) => api<VersionDetail>(`/api/versions/${id}`);
export const getPreview = (id: string) => api<VersionPreview>(`/api/versions/${id}/preview`);
