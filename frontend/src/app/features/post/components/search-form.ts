import { ChangeDetectionStrategy, Component, input, output } from "@angular/core";
import { ButtonComponent } from "@/shared/ui/button";

@Component({
  selector: "app-search-form",
  imports: [ButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="flex gap-2" (submit)="submit($event)">
      <input
        name="keyword"
        [value]="keyword()"
        placeholder="제목이나 본문으로 검색"
        aria-label="검색어"
        class="flex-1 rounded border border-slate-300 px-3 py-2"
      />
      <app-button type="submit">검색</app-button>
    </form>
  `,
})
export class SearchFormComponent {
  readonly keyword = input("");
  readonly searched = output<string>();

  protected submit(event: Event): void {
    event.preventDefault();
    const data = new FormData(event.target as HTMLFormElement);
    this.searched.emit(String(data.get("keyword") ?? ""));
  }
}
