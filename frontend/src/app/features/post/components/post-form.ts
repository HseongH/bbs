import { ChangeDetectionStrategy, Component, effect, inject, input, output } from "@angular/core";
import { FormBuilder, ReactiveFormsModule } from "@angular/forms";
import { ButtonComponent } from "@/shared/ui/button";

@Component({
  selector: "app-post-form",
  imports: [ReactiveFormsModule, ButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form [formGroup]="form" class="space-y-4" (ngSubmit)="submit()">
      <div class="space-y-1">
        <label for="title" class="block text-sm font-medium">제목</label>
        <input
          id="title"
          formControlName="title"
          class="w-full rounded border border-slate-300 px-3 py-2"
        />
        @if (fieldErrors()["title"]; as message) {
          <p class="text-sm text-red-600">{{ message }}</p>
        }
      </div>

      <div class="space-y-1">
        <label for="content" class="block text-sm font-medium">본문</label>
        <textarea
          id="content"
          formControlName="content"
          rows="12"
          class="w-full rounded border border-slate-300 px-3 py-2"
        ></textarea>
        @if (fieldErrors()["content"]; as message) {
          <p class="text-sm text-red-600">{{ message }}</p>
        }
      </div>

      @if (message()) {
        <p class="text-sm text-red-600">{{ message() }}</p>
      }

      <app-button type="submit" [disabled]="submitting()">저장</app-button>
    </form>
  `,
})
export class PostFormComponent {
  readonly initial = input.required<{ title: string; content: string }>();
  readonly submitting = input(false);
  readonly fieldErrors = input<Record<string, string>>({});
  readonly message = input<string | null>(null);
  readonly saved = output<{ title: string; content: string }>();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    title: "",
    content: "",
  });

  constructor() {
    effect(() => {
      this.form.setValue(this.initial());
    });
  }

  protected submit(): void {
    this.saved.emit(this.form.getRawValue());
  }
}
