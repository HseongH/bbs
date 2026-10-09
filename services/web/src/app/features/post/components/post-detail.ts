import { ChangeDetectionStrategy, Component, input, output } from "@angular/core";
import { RouterLink } from "@angular/router";
import { ButtonComponent } from "@/shared/ui/button";
import type { PostDetail } from "../post-api.service";

@Component({
  selector: "app-post-detail",
  imports: [RouterLink, ButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (post(); as value) {
      <article class="space-y-4">
        <header class="space-y-2 border-b border-slate-200 pb-3">
          <h1 class="text-2xl font-semibold">{{ value.title }}</h1>
          <div class="flex items-center gap-3 text-sm text-slate-500">
            <span>조회 {{ value.viewCount }}</span>
            <ng-content select="[like]" />
          </div>
        </header>

        <p class="leading-relaxed whitespace-pre-wrap">{{ value.content }}</p>

        @if (canEdit()) {
          <div class="flex gap-2">
            <a
              [routerLink]="['/posts', value.id, 'edit']"
              class="rounded border border-slate-300 px-3 py-1 text-sm"
            >
              수정
            </a>
            <app-button variant="destructive" size="sm" (click)="removed.emit()">삭제</app-button>
          </div>
        }
      </article>
    }
  `,
})
export class PostDetailComponent {
  readonly post = input<PostDetail | undefined>();
  readonly canEdit = input(false);
  readonly removed = output<void>();
}
