/**
 * SmartReport E2E Playwright 配置
 * 用法: npx playwright test --config=test/scripts/e2e/playwright.config.ts
 */
import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: ".",
  timeout: 30000,
  retries: 1,
  use: {
    baseURL: "http://localhost:3000",
    headless: true,
    viewport: { width: 1920, height: 1080 },
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  projects: [
    {
      name: "Desktop Chrome",
      use: { browserName: "chromium" },
    },
  ],
});
