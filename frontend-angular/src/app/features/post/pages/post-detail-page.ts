import { ChangeDetectionStrategy, Component, computed, effect, inject, input } from "@angular/core";
import { Router } from "@angular/router";
import { isProblemCode, toProblem } from "@/core/api/problem";
import { CurrentMemberStore } from "@/core/auth/current-member.store";
import { CommentSectionComponent } from "@/features/comment/components/comment-section";
import { PostDetailComponent } from "../components/post-detail";
import { PostStore } from "../post.store";

@Component({
  selector: "app-post-detail-page",
  imports: [PostDetailComponent, CommentSectionComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (errorMessage(); as message) {
      <p class="py-8 text-center text-slate-600">{{ message }}</p>
    } @else if (store.detail.isLoading()) {
      <p class="py-8 text-center text-slate-500">불러오는 중입니다.</p>
    } @else {
      <app-post-detail [post]="store.detail.value()" [canEdit]="canEdit()" (removed)="remove()" />
    }
  `,
})
export class PostDetailPage {
  protected readonly store = inject(PostStore);
  protected readonly memberStore = inject(CurrentMemberStore);
  private readonly router = inject(Router);

  readonly postId = input.required<string>();

  protected readonly errorMessage = computed(() => {
    const error = this.store.detail.error();
    if (!error) {
      return null;
    }
    // HttpErrorResponse는 응답 본문을 error 속성에 담는다.
    const body = (error as { error?: unknown }).error ?? error;
    if (isProblemCode(body, "POST_NOT_FOUND")) {
      return "게시글을 찾을 수 없습니다.";
    }
    return toProblem(body)?.detail ?? "게시글을 불러오지 못했습니다.";
  });

  protected readonly canEdit = computed(() => {
    const member = this.memberStore.member();
    const post = this.store.detail.value();
    return member !== null && post !== undefined && member.id === post.authorId;
  });

  constructor() {
    effect(() => {
      this.store.select(Number(this.postId()));
    });
  }

  protected remove(): void {
    if (!window.confirm("게시글을 삭제할까요?")) {
      return;
    }
    void this.store.remove(Number(this.postId())).then(() => this.router.navigate(["/"]));
  }
}
