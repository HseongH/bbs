import { ChangeDetectionStrategy, Component, effect, inject, input } from "@angular/core";
import { Router, RouterLink } from "@angular/router";
import { PaginationComponent } from "../components/pagination";
import { PostListComponent } from "../components/post-list";
import { SearchFormComponent } from "../components/search-form";
import { PostStore } from "../post.store";

function 정수로(value: string | undefined, 기본값: number, 최댓값: number): number {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 && parsed <= 최댓값 ? parsed : 기본값;
}

@Component({
  selector: "app-post-list-page",
  imports: [PostListComponent, SearchFormComponent, PaginationComponent, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-4">
      <div class="flex justify-end">
        <a routerLink="/posts/new" class="rounded bg-slate-900 px-4 py-2 text-sm text-white">
          글쓰기
        </a>
      </div>
      <app-search-form [keyword]="keyword() ?? ''" (searched)="onSearch($event)" />
      <app-post-list
        [page]="store.list.value()"
        [isLoading]="store.list.isLoading()"
        [hasError]="store.list.error() !== undefined"
      />
      <app-pagination
        [page]="currentPage()"
        [totalPages]="store.list.value()?.totalPages ?? 0"
        (changed)="onPage($event)"
      />
    </div>
  `,
})
export class PostListPage {
  protected readonly store = inject(PostStore);
  private readonly router = inject(Router);

  readonly page = input<string>();
  readonly size = input<string>();
  readonly keyword = input<string>();

  constructor() {
    // URL이 상태의 출처다. 값이 바뀌면 스토어에 반영하고 스토어가 다시 불러온다.
    effect(() => {
      const keyword = this.keyword()?.trim();
      this.store.setSearch({
        page: 정수로(this.page(), 0, 10000),
        size: 정수로(this.size(), 20, 100) || 20,
        ...(keyword ? { keyword } : {}),
      });
    });
  }

  protected currentPage(): number {
    return 정수로(this.page(), 0, 10000);
  }

  protected onSearch(keyword: string): void {
    void this.router.navigate([], {
      queryParams: { page: 0, keyword: keyword.trim() || null },
      queryParamsHandling: "merge",
    });
  }

  protected onPage(page: number): void {
    void this.router.navigate([], { queryParams: { page }, queryParamsHandling: "merge" });
  }
}
