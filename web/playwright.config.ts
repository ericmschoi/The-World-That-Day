import { defineConfig, devices } from '@playwright/test'

// E2E in a real Chromium (plan M3b2b "Playwright 설정"). Only our dev server
// runs: every `/api` request and every request leaving localhost is
// intercepted in the tests (e2e/network.ts), so no 8080 server is needed.
const port = 5173

export default defineConfig({
  testDir: './e2e',
  // Flaky tests stay visible.
  retries: 0,
  forbidOnly: Boolean(process.env.CI),
  reporter: 'list',
  use: {
    baseURL: `http://localhost:${port}`,
    // Interception must see every request, even from a future service worker.
    serviceWorkers: 'block',
    trace: 'off',
  },
  projects: [360, 1280].map((width) => ({
    name: `chromium-${width}`,
    use: { ...devices['Desktop Chrome'], viewport: { width, height: 800 } },
  })),
  webServer: {
    command: 'npm run dev',
    url: `http://localhost:${port}`,
    reuseExistingServer: !process.env.CI,
  },
})
