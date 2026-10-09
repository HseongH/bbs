import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { SearchFormComponent } from "./search-form";

describe("SearchFormComponent", () => {
  it("현재 검색어를 입력란에 채운다", async () => {
    await render(SearchFormComponent, { inputs: { keyword: "앵귤러" } });

    expect(screen.getByLabelText("검색어")).toHaveValue("앵귤러");
  });

  it("입력한 검색어를 전달한다", async () => {
    const searched = vi.fn();
    await render(SearchFormComponent, { inputs: { keyword: "" }, on: { searched } });

    await userEvent.type(screen.getByLabelText("검색어"), "새 검색어");
    await userEvent.click(screen.getByRole("button", { name: "검색" }));

    expect(searched).toHaveBeenCalledWith("새 검색어");
  });
});
