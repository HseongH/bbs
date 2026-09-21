import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { RouterLink, RouterOutlet } from "@angular/router";
import { LOGIN_URL } from "@/core/auth/auth.interceptor";
import { CurrentMemberStore } from "@/core/auth/current-member.store";

@Component({
  selector: "app-root",
  imports: [RouterLink, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="border-b border-slate-200">
      <div class="mx-auto flex max-w-4xl items-center justify-between px-4 py-3">
        <a routerLink="/" class="text-lg font-semibold">게시판</a>
        <nav class="flex items-center gap-3 text-sm">
          @if (!store.isLoading()) {
            @if (store.member(); as member) {
              <span class="text-slate-700">{{ member.nickname }}</span>
              <button type="button" class="text-slate-500 hover:text-slate-900" (click)="logout()">
                로그아웃
              </button>
            } @else {
              <a [href]="loginUrl" class="text-slate-500 hover:text-slate-900">로그인</a>
            }
          }
        </nav>
      </div>
    </header>
    <main class="mx-auto max-w-4xl px-4 py-6">
      <router-outlet />
    </main>
  `,
})
export class App {
  protected readonly store = inject(CurrentMemberStore);
  protected readonly loginUrl = LOGIN_URL;

  protected logout(): void {
    void this.store.logout();
  }
}
