import { defineConfig } from '@playwright/test';
import path from 'node:path';

const evidenceDir = process.env.MODULE_A_EVIDENCE_DIR
  ?? path.resolve('test-results/module-a');

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 25000,
  expect: { timeout: 4000 },
  outputDir: path.join(evidenceDir, 'browser-artifacts'),
  reporter: [
    ['list'],
    ['json', { outputFile: path.join(evidenceDir, 'playwright-results.json') }],
  ],
  use: {
    baseURL: process.env.MODULE_A_BASE_URL ?? 'http://127.0.0.1:5173',
    viewport: { width: 1440, height: 1000 },
    locale: 'vi-VN',
    timezoneId: 'Asia/Bangkok',
    browserName: 'chromium',
  },
});
