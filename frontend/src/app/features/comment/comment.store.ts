import { httpResource } from "@angular/common/http";
import { inject, Injectable, signal } from "@angular/core";
import { firstValueFrom } from "rxjs";
import { CommentApiService, type CommentPage } from "./comment-api.service";

/** 재조회 범위를 이 서비스가 소유한다. */
@Injectable({ providedIn: "root" })
export class CommentStore {
  private readonly api = inject(CommentApiService);
  private readonly postId = signal<number | null>(null);

  /** 게시판 규모에서 댓글 페이지네이션은 UI만 복잡하게 만든다. 한 번에 가져온다. */
  readonly list = httpResource<CommentPage>(() => {
    const id = this.postId();
    return id === null
      ? undefined
      : { url: `/api/posts/${id}/comments`, params: { page: 0, size: 100 } };
  });

  select(postId: number | null): void {
    this.postId.set(postId);
  }

  async write(input: { body: string; parentCommentId?: number }): Promise<void> {
    const id = this.postId();
    if (id === null) {
      throw new Error("게시글이 선택되지 않았습니다.");
    }
    await firstValueFrom(this.api.write(id, input));
    this.list.reload();
  }

  async update(id: number, body: string): Promise<void> {
    await firstValueFrom(this.api.update(id, body));
    this.list.reload();
  }

  async remove(id: number): Promise<void> {
    await firstValueFrom(this.api.remove(id));
    this.list.reload();
  }
}
