import { Component, input, linkedSignal, output } from "@angular/core";
import { form, FormField, FormRoot } from "@angular/forms/signals";
import { ButtonComponent } from "@/shared/ui/button";

@Component({
  selector: "app-search-form",
  imports: [FormRoot, FormField, ButtonComponent],
  template: `
    <form [formRoot]="form" class="flex gap-2">
      <input
        [formField]="form.keyword"
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

  /** URL의 검색어가 바뀌면(뒤로 가기 등) 입력란도 따라간다. */
  private readonly model = linkedSignal(() => ({ keyword: this.keyword() }));

  protected readonly form = form(this.model, {
    submission: {
      action: async () => {
        this.searched.emit(this.model().keyword);
      },
    },
  });
}
