import { renderHook, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";
import { queryWrapper } from "@/test/render";
import { server } from "@/test/setup";
import { useCurrentMember } from "./queries";

describe("useCurrentMember", () => {
  it("로그인 상태면 회원 정보를 반환한다", async () => {
    const { result } = renderHook(() => useCurrentMember(), { wrapper: queryWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.nickname).toBe("테스터");
  });

  it("미인증이면 null을 반환하고 로그인으로 이동하지 않는다", async () => {
    server.use(
      http.get("/api/members/me", () =>
        HttpResponse.json({ status: 401, code: "UNAUTHENTICATED" }, { status: 401 }),
      ),
    );

    const { result } = renderHook(() => useCurrentMember(), { wrapper: queryWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBeNull();
  });
});
