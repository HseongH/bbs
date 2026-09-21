import { describe, expect, it } from "vitest";
import { fieldErrors, isProblemCode, toProblem } from "./problem";

const 권한오류 = {
  type: "urn:bbs:error:access_denied",
  title: "Forbidden",
  status: 403,
  detail: "권한이 없습니다.",
  instance: "/api/posts/2",
  code: "ACCESS_DENIED",
};

const 검증오류 = {
  type: "urn:bbs:error:invalid_request",
  title: "Bad Request",
  status: 400,
  detail: "요청 값이 올바르지 않습니다.",
  instance: "/api/posts",
  code: "INVALID_REQUEST",
  errors: { title: "제목은 필수입니다." },
};

describe("toProblem", () => {
  it("ProblemDetail 형태를 인식한다", () => {
    expect(toProblem(권한오류)?.code).toBe("ACCESS_DENIED");
  });

  it("형태가 다르면 null을 반환한다", () => {
    expect(toProblem({ message: "그냥 오류" })).toBeNull();
    expect(toProblem(null)).toBeNull();
    expect(toProblem("문자열")).toBeNull();
  });
});

describe("isProblemCode", () => {
  it("코드가 일치하면 참이다", () => {
    expect(isProblemCode(권한오류, "ACCESS_DENIED")).toBe(true);
    expect(isProblemCode(권한오류, "POST_NOT_FOUND")).toBe(false);
  });
});

describe("fieldErrors", () => {
  it("검증 오류의 필드별 메시지를 꺼낸다", () => {
    expect(fieldErrors(검증오류)).toEqual({ title: "제목은 필수입니다." });
  });

  it("검증 오류가 아니면 빈 객체다", () => {
    expect(fieldErrors(권한오류)).toEqual({});
  });
});
