import { HttpClient, HttpContext, httpResource } from "@angular/common/http";
import { computed, inject, Injectable } from "@angular/core";
import { toObservable } from "@angular/core/rxjs-interop";
import { filter, firstValueFrom } from "rxjs";
import type { components } from "@/core/api/schema";
import { SKIP_LOGIN_REDIRECT } from "./auth.interceptor";

export type Member = components["schemas"]["MemberResponse"];

@Injectable({ providedIn: "root" })
export class CurrentMemberStore {
  private readonly http = inject(HttpClient);

  /** 401은 로그인하지 않았다는 정상 응답이므로 인터셉터를 건너뛴다. */
  private readonly resource = httpResource<Member>(() => ({
    url: "/api/members/me",
    context: new HttpContext().set(SKIP_LOGIN_REDIRECT, true),
  }));

  readonly member = computed<Member | null>(() =>
    this.resource.error() ? null : (this.resource.value() ?? null),
  );
  readonly isLoading = this.resource.isLoading;

  /** 첫 응답 전에는 값이 없다. 아직 모르는 상태와 비로그인을 구분하려면 결론이 날 때까지 기다려야 한다. */
  private readonly settled$ = toObservable(
    computed(() => {
      const status = this.resource.status();
      return status === "resolved" || status === "error" || status === "local";
    }),
  ).pipe(filter(Boolean));

  /**
   * 회원 조회가 끝난 뒤의 결과를 돌려준다.
   *
   * @return 로그인한 회원, 비로그인이면 null
   */
  async whenSettled(): Promise<Member | null> {
    await firstValueFrom(this.settled$);
    return this.member();
  }

  reload(): void {
    this.resource.reload();
  }

  async logout(): Promise<void> {
    await firstValueFrom(this.http.post("/logout", null, { responseType: "text" }));
    window.location.href = "/";
  }
}
