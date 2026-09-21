import "@testing-library/jest-dom/vitest";
import { TestBed } from "@angular/core/testing";
import { setupServer } from "msw/node";
import { handlers } from "./handlers";

export const server = setupServer(...handlers);

beforeAll(() => {
  server.listen({ onUnhandledRequest: "error" });
});

afterEach(() => {
  // 루트 주입기에 만들어진 리소스가 다음 테스트로 새지 않도록 매번 초기화한다.
  TestBed.resetTestingModule();
  server.resetHandlers();
});

afterAll(() => {
  server.close();
});
