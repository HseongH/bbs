import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from "@angular/core";
import { Router } from "@angular/router";
import { fieldErrors, toProblem } from "@/core/api/problem";
import { PostFormComponent } from "../components/post-form";
import { PostStore } from "../post.store";

@Component({
  selector: "app-post-edit-page",
  imports: [PostFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <!-- 오류 상태에서 value()를 읽으면 예외가 나므로 먼저 걸러낸다. -->
    @if (store.detail.error()) {
      <p class="py-8 text-center text-slate-600">게시글을 불러오지 못했습니다.</p>
    } @else if (store.detail.value(); as post) {
      <app-post-form
        [initial]="{ title: post.title, content: post.content }"
        [submitting]="submitting()"
        [fieldErrors]="errors()"
        [message]="message()"
        (saved)="save($event)"
      />
    } @else {
      <p class="py-8 text-center text-slate-500">불러오는 중입니다.</p>
    }
  `,
})
export class PostEditPage {
  protected readonly store = inject(PostStore);
  private readonly router = inject(Router);

  readonly postId = input.required<string>();

  protected readonly submitting = signal(false);
  protected readonly errors = signal<Record<string, string>>({});
  protected readonly message = signal<string | null>(null);

  constructor() {
    effect(() => {
      this.store.select(Number(this.postId()));
    });
  }

  protected async save(input: { title: string; content: string }): Promise<void> {
    this.submitting.set(true);
    this.errors.set({});
    this.message.set(null);
    try {
      await this.store.update(Number(this.postId()), input);
      await this.router.navigate(["/posts", this.postId()]);
    } catch (error) {
      const body = (error as { error?: unknown }).error ?? error;
      this.errors.set(fieldErrors(body));
      this.message.set(toProblem(body)?.detail ?? "저장하지 못했습니다.");
    } finally {
      this.submitting.set(false);
    }
  }
}
