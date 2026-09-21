import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from "@angular/core";
import { CurrentMemberStore } from "@/core/auth/current-member.store";
import type { Comment } from "../comment-api.service";
import { CommentStore } from "../comment.store";
import { CommentFormComponent } from "./comment-form";

type ViewMode = "view" | "edit" | "reply";

@Component({
  selector: "app-comment-item",
  imports: [CommentFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <li [class]="comment().depth > 0 ? 'border-l-2 border-slate-200 py-2 pl-6' : 'py-2'">
      @if (mode() === "edit") {
        <app-comment-form
          label="댓글 수정"
          submitLabel="수정"
          [initial]="comment().body"
          (saved)="applyEdit($event)"
        />
      } @else {
        <p class="text-sm whitespace-pre-wrap">{{ comment().body }}</p>
      }

      <div class="mt-1 flex gap-2 text-xs text-slate-500">
        @if (canReply()) {
          <button type="button" (click)="toggle('reply')">답글 달기</button>
        }
        @if (isMine()) {
          <button type="button" (click)="toggle('edit')">수정</button>
          <button type="button" class="text-red-600" (click)="remove()">삭제</button>
        }
      </div>

      @if (mode() === "reply") {
        <div class="mt-2 pl-6">
          <app-comment-form label="답글" submitLabel="등록" (saved)="applyReply($event)" />
        </div>
      }
    </li>
  `,
})
export class CommentItemComponent {
  readonly comment = input.required<Comment>();

  private readonly store = inject(CommentStore);
  private readonly memberStore = inject(CurrentMemberStore);

  protected readonly mode = signal<ViewMode>("view");

  protected readonly isMine = computed(
    () => this.memberStore.member()?.id === this.comment().authorId,
  );

  /** 답글의 답글은 서버가 400으로 거부한다. 버튼을 내보내지 않아 그 전에 막는다. */
  protected readonly canReply = computed(
    () => this.memberStore.member() !== null && this.comment().depth === 0,
  );

  protected toggle(target: ViewMode): void {
    this.mode.update((current) => (current === target ? "view" : target));
  }

  protected applyEdit(body: string): void {
    void this.store.update(this.comment().id, body).then(() => this.mode.set("view"));
  }

  protected applyReply(body: string): void {
    void this.store
      .write({ body, parentCommentId: this.comment().id })
      .then(() => this.mode.set("view"));
  }

  protected remove(): void {
    if (window.confirm("댓글을 삭제할까요?")) {
      void this.store.remove(this.comment().id);
    }
  }
}
