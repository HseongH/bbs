import { ChangeDetectionStrategy, Component, inject, signal } from "@angular/core";
import { Router } from "@angular/router";
import { fieldErrors, toProblem } from "@/core/api/problem";
import { PostFormComponent } from "../components/post-form";
import { PostStore } from "../post.store";

@Component({
  selector: "app-post-new-page",
  imports: [PostFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-post-form
      [initial]="{ title: '', content: '' }"
      [submitting]="submitting()"
      [fieldErrors]="errors()"
      [message]="message()"
      (saved)="save($event)"
    />
  `,
})
export class PostNewPage {
  private readonly store = inject(PostStore);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly errors = signal<Record<string, string>>({});
  protected readonly message = signal<string | null>(null);

  protected async save(input: { title: string; content: string }): Promise<void> {
    this.submitting.set(true);
    this.errors.set({});
    this.message.set(null);
    try {
      await this.store.create(input);
      await this.router.navigate(["/"]);
    } catch (error) {
      const body = (error as { error?: unknown }).error ?? error;
      this.errors.set(fieldErrors(body));
      this.message.set(toProblem(body)?.detail ?? "저장하지 못했습니다.");
    } finally {
      this.submitting.set(false);
    }
  }
}
