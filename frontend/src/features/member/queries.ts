import { useQuery } from "@tanstack/react-query";
import type { components } from "@/api/schema";

export type Member = components["schemas"]["MemberResponse"];

export const memberKeys = {
  all: ["member"] as const,
  me: () => [...memberKeys.all, "me"] as const,
};

/**
 * 로그인 여부 확인은 401을 정상 응답으로 취급한다.
 *
 * <p>공용 클라이언트는 401을 받으면 로그인 화면으로 보낸다. 로그인 여부를 확인하는 이 요청까지 그렇게 처리하면 비로그인 사용자가 아무 화면도 볼 수 없으므로 fetch를
 * 직접 쓴다.
 */
export function useCurrentMember() {
  return useQuery({
    queryKey: memberKeys.me(),
    queryFn: async (): Promise<Member | null> => {
      const response = await fetch("/api/members/me", { headers: { Accept: "application/json" } });
      if (response.status === 401) {
        return null;
      }
      if (!response.ok) {
        throw new Error(`회원 정보를 가져오지 못했습니다. (${response.status})`);
      }
      return (await response.json()) as Member;
    },
    staleTime: 5 * 60 * 1000,
  });
}
