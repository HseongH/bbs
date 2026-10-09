import { defineConfig } from "@playwright/test";

const 저장된로그인 = "e2e/.auth/user.json";

// 인증 흐름은 Keycloak에 등록된 주소로만 동작하므로 앱과 같은 환경 변수를 따른다.
const host = process.env.BBS_HOST ?? "localhost";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  // 모든 시나리오가 같은 데이터베이스를 공유하므로 병렬로 돌리면 서로의 데이터를 밟는다.
  workers: 1,
  reporter: "list",
  // CI에서는 test.only가 남아 나머지 시나리오를 건너뛰는 일을 막는다.
  forbidOnly: !!process.env.CI,
  use: {
    // 브라우저는 진입점(Traefik) 한 곳으로만 접속한다 (COM-CON-003).
    baseURL: `http://${host}:8000`,
    // CI는 재시도하지 않으므로 실패한 시나리오의 trace를 남겨 원인을 확인한다.
    trace: process.env.CI ? "retain-on-failure" : "on-first-retry",
  },
  // 브라우저는 Playwright의 기본값(Chromium)을 쓴다. 브라우저를 늘릴 때만 프로젝트마다 지정한다.
  projects: [
    { name: "setup", testMatch: /auth\.setup\.ts/ },
    { name: "e2e", use: { storageState: 저장된로그인 }, dependencies: ["setup"] },
  ],
  // 화면 개발 서버는 진입점 뒤에서 화면 경로를 맡는다.
  webServer: {
    command: "pnpm dev",
    url: `http://${host}:5173`,
    reuseExistingServer: true,
    timeout: 60_000,
  },
});
