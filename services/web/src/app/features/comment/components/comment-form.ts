import { Component, input, linkedSignal, output } from "@angular/core";
import { form, FormField, FormRoot } from "@angular/forms/signals";
import { ButtonComponent } from "@/shared/ui/button";

@Component({
  selector: "app-comment-form",
  imports: [FormRoot, FormField, ButtonComponent],
  template: `
    <form [formRoot]="form" class="space-y-1">
      <textarea
        rows="3"
        [formField]="form.body"
        [attr.aria-label]="label()"
        class="w-full rounded border border-slate-300 px-3 py-2 text-sm"
      ></textarea>
      @if (error(); as message) {
        <p class="text-sm text-red-600">{{ message }}</p>
      }
      <app-button type="submit" size="sm" [disabled]="submitting()">{{ submitLabel() }}</app-button>
    </form>
  `,
})
export class CommentFormComponent {
  readonly label = input.required<string>();
  readonly submitLabel = input.required<string>();
  readonly initial = input("");
  readonly error = input<string | null>(null);
  readonly submitting = input(false);
  readonly saved = output<string>();

  private readonly model = linkedSignal(() => ({ body: this.initial() }));

  protected readonly form = form(this.model, {
    submission: {
      action: async () => {
        this.saved.emit(this.model().body);
      },
    },
  });

  /** 등록에 성공했을 때 부모가 호출한다. 실패하면 사용자가 쓴 내용을 잃지 않도록 그대로 둔다. */
  reset(): void {
    this.model.set({ body: this.initial() });
    this.form().reset();
  }
}
