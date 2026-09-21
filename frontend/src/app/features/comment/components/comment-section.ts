import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  input,
  viewChild,
} from "@angular/core";
import { CurrentMemberStore } from "@/core/auth/current-member.store";
import { CommentStore } from "../comment.store";
import { CommentFormComponent } from "./comment-form";
import { CommentItemComponent } from "./comment-item";

@Component({
  selector: "app-comment-section",
  imports: [CommentFormComponent, CommentItemComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="space-y-3 border-t border-slate-200 pt-4">
      <h2 class="text-lg font-medium">댓글 {{ store.list.value()?.totalElements ?? 0 }}</h2>

      @if (memberStore.member()) {
        <app-comment-form label="댓글" submitLabel="등록" (saved)="write($event)" />
      } @else {
        <p class="text-sm text-slate-500">댓글을 쓰려면 로그인이 필요합니다.</p>
      }

      @if (store.list.isLoading()) {
        <p class="text-sm text-slate-500">불러오는 중입니다.</p>
      } @else if (store.list.error()) {
        <p class="text-sm text-red-600">댓글을 불러오지 못했습니다.</p>
      }

      <ul class="divide-y divide-slate-100">
        @for (comment of store.list.value()?.content ?? []; track comment.id) {
          <app-comment-item [comment]="comment" />
        }
      </ul>
    </section>
  `,
})
export class CommentSectionComponent {
  readonly postId = input.required<number>();

  protected readonly store = inject(CommentStore);
  protected readonly memberStore = inject(CurrentMemberStore);

  constructor() {
    effect(() => {
      this.store.select(this.postId());
    });
  }

  private readonly form = viewChild(CommentFormComponent);

  protected async write(body: string): Promise<void> {
    await this.store.write({ body });
    this.form()?.reset();
  }
}
