import { httpResource } from "@angular/common/http";
import { inject, Injectable, signal } from "@angular/core";
import { firstValueFrom } from "rxjs";
import { PostApiService, type PostDetail, type PostPage } from "./post-api.service";

export interface PostListSearch {
  page: number;
  size: number;
  keyword?: string;
}

/** 재조회 범위를 이 서비스가 소유한다. 컴포넌트가 직접 reload를 부르면 범위가 흩어진다. */
@Injectable({ providedIn: "root" })
export class PostStore {
  private readonly api = inject(PostApiService);

  private readonly search = signal<PostListSearch | null>(null);
  private readonly selectedId = signal<number | null>(null);

  /** 검색 조건이 정해지기 전에는 요청하지 않는다. URL을 읽기도 전에 기본 조건으로 한 번 더 부르는 낭비를 막는다. */
  readonly list = httpResource<PostPage>(() => {
    const search = this.search();
    if (search === null) {
      return undefined;
    }
    const keyword = search.keyword?.trim();
    return {
      url: "/api/posts",
      params: { page: search.page, size: search.size, ...(keyword ? { keyword } : {}) },
    };
  });

  readonly detail = httpResource<PostDetail>(() => {
    const id = this.selectedId();
    return id === null ? undefined : { url: `/api/posts/${id}` };
  });

  setSearch(search: PostListSearch): void {
    this.search.set(search);
  }

  select(id: number | null): void {
    this.selectedId.set(id);
  }

  async create(input: { title: string; content: string }): Promise<void> {
    await firstValueFrom(this.api.create(input));
    this.list.reload();
  }

  async update(id: number, input: { title: string; content: string }): Promise<void> {
    await firstValueFrom(this.api.update(id, input));
    this.detail.reload();
    this.list.reload();
  }

  async remove(id: number): Promise<void> {
    await firstValueFrom(this.api.remove(id));
    this.list.reload();
  }
}
