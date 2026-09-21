import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { PostFormComponent } from "./post-form";

describe("PostFormComponent", () => {
  it("입력한 값을 전달한다", async () => {
    const saved = vi.fn();

    await render(PostFormComponent, {
      inputs: { initial: { title: "", content: "" }, submitting: false, fieldErrors: {} },
      on: { saved },
    });

    await userEvent.type(screen.getByLabelText("제목"), "새 글");
    await userEvent.type(screen.getByLabelText("본문"), "내용입니다");
    await userEvent.click(screen.getByRole("button", { name: "저장" }));

    expect(saved).toHaveBeenCalledWith({ title: "새 글", content: "내용입니다" });
  });

  it("서버 검증 오류를 필드 아래에 보여준다", async () => {
    await render(PostFormComponent, {
      inputs: {
        initial: { title: "", content: "" },
        submitting: false,
        fieldErrors: { title: "제목은 필수입니다." },
      },
    });

    expect(screen.getByText("제목은 필수입니다.")).toBeInTheDocument();
  });

  it("수정 모드에서는 기존 값이 채워진다", async () => {
    await render(PostFormComponent, {
      inputs: {
        initial: { title: "기존 제목", content: "기존 본문" },
        submitting: false,
        fieldErrors: {},
      },
    });

    expect(screen.getByLabelText("제목")).toHaveValue("기존 제목");
    expect(screen.getByLabelText("본문")).toHaveValue("기존 본문");
  });
});
