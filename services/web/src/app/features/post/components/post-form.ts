import { Component, computed, input, linkedSignal, output } from "@angular/core";
import { form, FormField, FormRoot } from "@angular/forms/signals";
import { ButtonComponent } from "@/shared/ui/button";

@Component({
  selector: "app-post-form",
  imports: [FormRoot, FormField, ButtonComponent],
  template: `
    <form [formRoot]="form" class="space-y-4">
      <div class="space-y-1">
        <label for="title" class="block text-sm font-medium">제목</label>
        <input
          id="title"
          [formField]="form.title"
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
          [formField]="form.content"
          rows="12"
          class="w-full rounded border border-slate-300 px-3 py-2"
        ></textarea>
        @if (fieldErrors()["content"]; as message) {
          <p class="text-sm text-red-600">{{ message }}</p>
        }
      </div>

      @if (generalMessage(); as text) {
        <p class="text-sm text-red-600">{{ text }}</p>
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

  /** 필드별 오류가 이미 붙어 있으면 같은 내용을 한 번 더 보여줄 이유가 없다. */
  protected readonly generalMessage = computed(() =>
    Object.keys(this.fieldErrors()).length === 0 ? this.message() : null,
  );

  /** 기존 값(수정 화면)이 바뀌면 입력값도 그 값으로 다시 맞춘다. */
  private readonly model = linkedSignal(() => this.initial());

  /** 검증은 서버가 하고 결과는 fieldErrors로 받는다. 저장은 부모가 맡는다. */
  protected readonly form = form(this.model, {
    submission: {
      action: async () => {
        this.saved.emit(this.model());
      },
    },
  });
}
