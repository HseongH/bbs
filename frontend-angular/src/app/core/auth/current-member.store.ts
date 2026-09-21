import { HttpClient, HttpContext, httpResource } from "@angular/common/http";
import { computed, inject, Injectable } from "@angular/core";
import { firstValueFrom } from "rxjs";
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

  reload(): void {
    this.resource.reload();
  }

  async logout(): Promise<void> {
    await firstValueFrom(this.http.post("/logout", null, { responseType: "text" }));
    window.location.href = "/";
  }
}
