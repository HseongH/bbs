import { ChangeDetectionStrategy, Component, input, output } from "@angular/core";
import { ButtonComponent } from "@/shared/ui/button";

@Component({
  selector: "app-comment-form",
  imports: [ButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="space-y-1" (submit)="submit($event)">
      <textarea
        name="body"
        rows="3"
        [value]="initial()"
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

  protected submit(event: Event): void {
    event.preventDefault();
    const data = new FormData(event.target as HTMLFormElement);
    this.saved.emit(String(data.get("body") ?? ""));
  }
}
