import { ChangeDetectionStrategy, Component, computed, input, output } from "@angular/core";
import { ButtonComponent } from "@/shared/ui/button";

@Component({
  selector: "app-pagination",
  imports: [ButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (totalPages() > 1) {
      <nav class="flex justify-center gap-1 py-4" aria-label="페이지">
        @for (index of pages(); track index) {
          <app-button
            size="sm"
            [variant]="index === page() ? 'default' : 'ghost'"
            [ariaCurrent]="index === page() ? 'page' : null"
            (click)="changed.emit(index)"
          >
            {{ index + 1 }}
          </app-button>
        }
      </nav>
    }
  `,
})
export class PaginationComponent {
  readonly page = input(0);
  readonly totalPages = input(0);
  readonly changed = output<number>();

  protected readonly pages = computed(() =>
    Array.from({ length: this.totalPages() }, (_, index) => index),
  );
}
