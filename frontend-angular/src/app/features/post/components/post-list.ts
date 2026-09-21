import { ChangeDetectionStrategy, Component, input } from "@angular/core";
import { RouterLink } from "@angular/router";
import type { PostPage } from "../post-api.service";

@Component({
  selector: "app-post-list",
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (isLoading()) {
      <p class="py-8 text-center text-slate-500">불러오는 중입니다.</p>
    } @else if (hasError()) {
      <p class="py-8 text-center text-red-600">목록을 불러오지 못했습니다.</p>
    } @else if ((page()?.content ?? []).length === 0) {
      <p class="py-8 text-center text-slate-500">게시글이 없습니다.</p>
    } @else {
      <ul class="divide-y divide-slate-200">
        @for (post of page()?.content ?? []; track post.id) {
          <li class="py-3">
            <a [routerLink]="['/posts', post.id]" class="font-medium hover:underline">
              {{ post.title }}
            </a>
            <div class="mt-1 flex gap-3 text-sm text-slate-500">
              <span>{{ post.authorNickname }}</span>
              <span>조회 {{ post.viewCount }}</span>
              <span>좋아요 {{ post.likeCount }}</span>
            </div>
          </li>
        }
      </ul>
    }
  `,
})
export class PostListComponent {
  readonly page = input<PostPage | undefined>();
  readonly isLoading = input(false);
  readonly hasError = input(false);
}
