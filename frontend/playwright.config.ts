import { defineConfig, devices } from "@playwright/test";

const 저장된로그인 = "e2e/.auth/user.json";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  // 모든 시나리오가 같은 데이터베이스를 공유하므로 병렬로 돌리면 서로의 데이터를 밟는다.
  workers: 1,
  reporter: "list",
  use: {
    baseURL: "http://localhost:5173",
    trace: "on-first-retry",
  },
  projects: [
    { name: "setup", testMatch: /auth\.setup\.ts/ },
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"], storageState: 저장된로그인 },
      dependencies: ["setup"],
    },
  ],
  webServer: {
    command: "pnpm dev",
    url: "http://localhost:5173",
    reuseExistingServer: true,
    timeout: 60_000,
  },
});
