import { defineConfig, devices } from "@playwright/test";

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
    baseURL: `http://${host}:5173`,
    // CI는 재시도하지 않으므로 실패한 시나리오의 trace를 남겨 원인을 확인한다.
    trace: process.env.CI ? "retain-on-failure" : "on-first-retry",
  },
  projects: [
    { name: "setup", testMatch: /auth\.setup\.ts/, use: { ...devices["Desktop Chrome"] } },
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"], storageState: 저장된로그인 },
      dependencies: ["setup"],
    },
  ],
  webServer: {
    command: "pnpm dev",
    url: `http://${host}:5173`,
    reuseExistingServer: true,
    timeout: 60_000,
  },
});
