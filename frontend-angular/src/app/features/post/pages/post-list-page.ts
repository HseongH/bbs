import { ChangeDetectionStrategy, Component } from "@angular/core";

@Component({
  selector: "app-post-list-page",
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<p>목록 준비 중</p>`,
})
export class PostListPage {}
