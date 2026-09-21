import { describe, expect, it } from "vitest";
import { cn } from "./utils";

describe("cn", () => {
  it("여러 클래스를 잇는다", () => {
    expect(cn("px-2", "py-1")).toBe("px-2 py-1");
  });

  it("거짓 값을 걸러낸다", () => {
    expect(cn("px-2", false, null, undefined)).toBe("px-2");
  });

  it("뒤에 온 Tailwind 클래스가 앞의 같은 속성을 덮는다", () => {
    expect(cn("px-2", "px-4")).toBe("px-4");
  });
});
