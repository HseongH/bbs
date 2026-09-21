import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from "@angular/core";
import { toProblem } from "@/core/api/problem";
import { ButtonComponent } from "@/shared/ui/button";
import { PostStore } from "../post.store";

@Component({
  selector: "app-like-button",
  imports: [ButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="inline-flex items-center gap-2">
      <app-button variant="outline" size="sm" (click)="press()">
        좋아요 {{ displayCount() }}
      </app-button>
      @if (message(); as text) {
        <span class="text-sm text-red-600">{{ text }}</span>
      }
    </span>
  `,
})
export class LikeButtonComponent {
  readonly postId = input.required<number>();
  readonly likeCount = input.required<number>();

  private readonly store = inject(PostStore);
  private readonly pending = signal(0);

  protected readonly message = signal<string | null>(null);

  /** 서버 응답 전에 먼저 보여 주고, 상세를 다시 불러오면 실제 값으로 맞춰진다. */
  protected readonly displayCount = computed(() => this.likeCount() + this.pending());

  protected press(): void {
    this.message.set(null);
    this.pending.set(1);
    void this.store
      .like(this.postId())
      .catch((error: unknown) => {
        const body = (error as { error?: unknown }).error ?? error;
        this.message.set(toProblem(body)?.detail ?? "좋아요에 실패했습니다.");
      })
      .finally(() => this.pending.set(0));
  }
}
