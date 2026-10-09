import { inject, Service } from "@angular/core";
import { Title } from "@angular/platform-browser";
import { type RouterStateSnapshot, TitleStrategy } from "@angular/router";

const 사이트이름 = "게시판";

/** 라우트의 title 뒤에 사이트 이름을 붙인다. 형식을 라우트마다 반복하지 않는다. */
@Service()
export class PageTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const page = this.buildTitle(snapshot);
    this.title.setTitle(page ? `${page} | ${사이트이름}` : 사이트이름);
  }
}
