import { test, expect } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';
import { readDemoPassword } from './demo-credential.js';

test.use({ trace: 'off', video: 'off' });
const root = process.env.MODULE_A_EVIDENCE_DIR ?? path.resolve('test-results/module-a-admin');
const apiBase = process.env.MODULE_A_ADMIN_API_URL ?? 'http://localhost:8080/api/v1';
const passwordFile = process.env.MODULE_A_DEMO_PASSWORD_FILE
  ?? 'E:/2.PTIT/QLDA/module-a-qa-runtime/private-demo-password.txt';
const records = new WeakMap();

test.beforeEach(async ({ page }, info) => {
  const directory = path.join(root, info.title.match(/TC\d+/)[0], info.title.replace(/[^a-zA-Z0-9-]+/g, '-'));
  await fs.mkdir(directory, { recursive: true });
  records.set(page, { directory, record: { tc: info.title.match(/TC\d+/)[0], title: info.title,
    startedAt: new Date().toISOString(), network: [], observations: {},
    authenticationEvidence: 'Real API login in untraced Node context. Passwords, tokens and authorization headers excluded.' } });
  page.on('response', (response) => {
    if (response.url().includes('/api/v1/')) records.get(page).record.network.push({
      method: response.request().method(), url: response.url(), status: response.status(),
    });
  });
});

test.afterEach(async ({ page }, info) => {
  const { record, directory } = records.get(page);
  record.status = info.status === 'passed' ? 'PASS' : 'FAIL';
  record.finishedAt = new Date().toISOString();
  record.errors = info.errors.map((error) => error.message);
  await page.screenshot({ path: path.join(directory, 'screenshot.png'), fullPage: true,
    mask: [page.locator('input[type=password]')] }).catch(() => {});
  await fs.writeFile(path.join(directory, 'result.json'), JSON.stringify(record, null, 2));
});

async function authenticate(page, request, username) {
  const password = await readDemoPassword(passwordFile);
  const response = await request.post(`${apiBase}/auth/login`, { data: { username, password } });
  expect(response.status(), 'Real local demo login status').toBe(200);
  const { data } = await response.json();
  await page.addInitScript(({ token, user }) => {
    localStorage.setItem('tour_discovery_token', token);
    localStorage.setItem('tour_discovery_user', JSON.stringify(user));
  }, { token: data.accessToken, user: { username: data.username, fullName: data.fullName, role: data.role } });
  return data.accessToken;
}

test('TC21 login requires configured password without embedded sample', async ({ page }) => {
  await page.goto('/admin');
  await expect(page.locator('#p')).toHaveValue('');
  await expect(page.locator('main')).not.toContainText('123456');
});

test('TC21 manager reads version without Product catalog or write controls', async ({ page, request }) => {
  await authenticate(page, request, 'manager01');
  await page.goto('/admin');
  await expect(page.getByRole('heading', { name: 'Danh mục sở thích', exact: true })).toHaveCount(0);
  await page.locator('#vid').fill('201');
  await page.getByRole('button', { name: 'Tải', exact: true }).click();
  await expect(page.locator('main')).toContainText('Đà Nẵng biển và ẩm thực');
  await expect(page.getByRole('button', { name: 'Lưu danh sách sở thích', exact: true })).toHaveCount(0);
  expect(records.get(page).record.network.some((r) => new URL(r.url).pathname === '/api/v1/internal/interests')).toBe(false);
});

test('TC21 editing version ID invalidates the previously loaded tag form', async ({ page, request }) => {
  await authenticate(page, request, 'product01');
  await page.goto('/admin');
  await page.getByRole('button', { name: 'Tải', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Lưu danh sách sở thích', exact: true })).toBeVisible();
  await page.locator('#vid').fill('201');
  await expect(page.getByRole('button', { name: 'Lưu danh sách sở thích', exact: true })).toHaveCount(0);
  expect(records.get(page).record.network.some((r) => r.method === 'PUT')).toBe(false);
});

test('TC43 historical inactive tags are visible and removable before replacement', async ({ page, request }) => {
  const token = await authenticate(page, request, 'product01');
  const headers = { Authorization: `Bearer ${token}` };
  const previous = await request.get(`${apiBase}/internal/tour-versions/215/interests`, { headers });
  const originalCodes = (await previous.json()).data.interests.map((i) => i.code);
  try {
    expect((await request.put(`${apiBase}/internal/tour-versions/215/interests`, {
      headers, data: { interestCodes: ['BEACH', 'CULTURE'] },
    })).status()).toBe(200);
    await page.goto('/admin');
    await page.getByRole('button', { name: 'Tải', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Lưu danh sách sở thích', exact: true })).toBeVisible();
    await page.getByRole('row').filter({ hasText: 'BEACH' }).getByRole('button', { name: 'Tắt', exact: true }).click();
    const inactive = page.getByRole('button', { name: 'Biển (đã tắt)', exact: true });
    await expect(inactive).toBeVisible();
    await expect(inactive).toHaveAttribute('aria-pressed', 'true');
    const save = page.getByRole('button', { name: 'Lưu danh sách sở thích', exact: true });
    await expect(save).toBeDisabled();
    await inactive.click();
    await expect(save).toBeEnabled();
    await save.click();
    await expect(page.locator('.notice.ok').filter({ hasText: 'Đã lưu 1 sở thích' })).toBeVisible();
    const current = await request.get(`${apiBase}/internal/tour-versions/215/interests`, { headers });
    const codes = (await current.json()).data.interests.map((i) => i.code);
    records.get(page).record.observations.savedCodes = codes;
    expect(codes).toEqual(['CULTURE']);
  } finally {
    await request.patch(`${apiBase}/internal/interests/1`, { headers, data: { active: true } });
    await request.put(`${apiBase}/internal/tour-versions/215/interests`, { headers, data: { interestCodes: originalCodes } });
  }
});
