export type ProblemDetail = {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  code: string;
  errors?: Record<string, string>;
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

/** 백엔드가 주는 RFC 9457 응답인지 판별한다. code와 status가 있으면 우리 규약을 따른 것이다. */
export function toProblem(error: unknown): ProblemDetail | null {
  if (!isRecord(error)) {
    return null;
  }
  if (typeof error.code !== "string" || typeof error.status !== "number") {
    return null;
  }
  return error as unknown as ProblemDetail;
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
