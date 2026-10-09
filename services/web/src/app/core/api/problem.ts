import { HttpErrorResponse } from "@angular/common/http";

export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  code: string;
  errors?: Record<string, string>;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

/**
 * 백엔드가 주는 RFC 9457 응답인지 판별한다. code와 status가 있으면 우리 규약을 따른 것이다.
 * HttpClient의 오류는 응답 본문을 error 속성에 담으므로, 호출하는 쪽은 받은 오류를 그대로 넘긴다.
 */
export function toProblem(error: unknown): ProblemDetail | null {
  const body = error instanceof HttpErrorResponse ? (error.error as unknown) : error;
  if (!isRecord(body)) {
    return null;
  }
  if (typeof body["code"] !== "string" || typeof body["status"] !== "number") {
    return null;
  }
  return body as unknown as ProblemDetail;
}

export function isProblemCode(error: unknown, code: string): boolean {
  return toProblem(error)?.code === code;
}

export function fieldErrors(error: unknown): Record<string, string> {
  const problem = toProblem(error);
  if (!problem?.errors) {
    return {};
  }
  return problem.errors;
}
