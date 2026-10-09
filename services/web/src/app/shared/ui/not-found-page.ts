import { ChangeDetectionStrategy, Component } from "@angular/core";

@Component({
  selector: "app-not-found-page",
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: ` <p class="py-16 text-center text-slate-600">요청하신 페이지를 찾을 수 없습니다.</p> `,
})
export class NotFoundPage {}
