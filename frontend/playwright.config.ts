import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests against the running app (frontend :5173 → BFF :8000 → API server :8090). Start all three
 * services first (README, "Running Services"), then `npm run e2e`. Browsers come from `npm run e2e:install`,
 * which installs the Chromium that matches the pinned @playwright/test version.
 */
export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts', // not *.test.ts, so Vitest leaves these alone
  fullyParallel: false,
  workers: 1,
  timeout: 60_000,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:5173',
    viewport: { width: 1600, height: 1000 },
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure'
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1600, height: 1000 } } }]
});
