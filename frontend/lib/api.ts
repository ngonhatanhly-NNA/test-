import type { ApiErrorBody } from "@/types";

export const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8000";

export class ApiError extends Error {
  constructor(
    public code: string,
    message: string,
    public status: number,
    public details: Record<string, unknown> = {},
  ) {
    super(message);
  }
}

export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const headers: Record<string, string> = {};
  if (typeof init?.body === "string") headers["Content-Type"] = "application/json"; // FormData sets its own
  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, { ...init, headers: { ...headers, ...init?.headers } });
  } catch {
    throw new ApiError("NETWORK_ERROR", "Không kết nối được máy chủ", 0);
  }
  if (res.status === 204) return undefined as T;
  const body: unknown = await res.json().catch(() => null);
  if (!res.ok) {
    const err = (body as ApiErrorBody | null)?.error;
    throw new ApiError(err?.code ?? "HTTP_ERROR", err?.message ?? "Có lỗi xảy ra", res.status, err?.details ?? {});
  }
  return body as T;
}

export const errorMessage = (e: unknown): string => (e instanceof Error ? e.message : "Có lỗi xảy ra");
