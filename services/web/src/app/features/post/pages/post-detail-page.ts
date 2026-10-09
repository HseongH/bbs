import { Component, computed, effect, inject, input, numberAttribute } from "@angular/core";
import { Router } from "@angular/router";
import { isProblemCode, toProblem } from "@/core/api/problem";
import { CurrentMemberStore } from "@/core/auth/current-member.store";
import { CommentSectionComponent } from "@/features/comment/components/comment-section";
import { LikeButtonComponent } from "../components/like-button";
import { PostDetailComponent } from "../components/post-detail";
import { PostStore } from "../post.store";

@Component({
  selector: "app-post-detail-page",
  imports: [PostDetailComponent, CommentSectionComponent, LikeButtonComponent],
  template: `
    @if (errorMessage(); as message) {
      <p class="py-8 text-center text-slate-600">{{ message }}</p>
    } @else if (store.detail.isLoading()) {
      <p class="py-8 text-center text-slate-500">불러오는 중입니다.</p>
    } @else {
      <app-post-detail [post]="store.detail.value()" [canEdit]="canEdit()" (removed)="remove()">
        @if (memberStore.member()) {
          <app-like-button
            like
            [postId]="postId()"
            [likeCount]="store.detail.value()?.likeCount ?? 0"
          />
        } @else {
          <span like>좋아요 {{ store.detail.value()?.likeCount ?? 0 }}</span>
        }
      </app-post-detail>
      <app-comment-section [postId]="postId()" />
    }
  `,
})
export class PostDetailPage {
  protected readonly store = inject(PostStore);
  protected readonly memberStore = inject(CurrentMemberStore);
  private readonly router = inject(Router);

  /** 경로 파라미터는 문자열로 들어온다. 숫자가 아니면 NaN이 되어 서버가 400으로 거부한다. */
  readonly postId = input.required({ transform: numberAttribute });

  protected readonly errorMessage = computed(() => {
    const error = this.store.detail.error();
    if (!error) {
      return null;
    }
    if (isProblemCode(error, "POST_NOT_FOUND")) {
      return "게시글을 찾을 수 없습니다.";
    }
    return toProblem(error)?.detail ?? "게시글을 불러오지 못했습니다.";
  });

  protected readonly canEdit = computed(() => {
    const member = this.memberStore.member();
    const post = this.store.detail.value();
    return member !== null && post !== undefined && member.id === post.authorId;
  });

  constructor() {
    effect(() => {
      this.store.select(this.postId());
    });
  }

  protected remove(): void {
    if (!window.confirm("게시글을 삭제할까요?")) {
      return;
    }
    void this.store.remove(this.postId()).then(() => this.router.navigate(["/"]));
  }
}
