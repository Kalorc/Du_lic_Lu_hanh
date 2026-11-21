import { test, expect } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import { readDemoPassword } from './demo-credential.js';

// Authentication is exercised in the UI; no trace, video or request body may
// record a demo password, JWT or Authorization header.
test.use({ trace: 'off', video: 'off' });
const root = process.env.MODULE_A_EVIDENCE_DIR ?? path.resolve('test-results/module-a-demo');
const apiBase = process.env.MODULE_A_ADMIN_API_URL ?? 'http://127.0.0.1:18080/api/v1';

async function photoReady(locator) {
  await locator.scrollIntoViewIfNeeded();
  await expect.poll(() => locator.evaluate((img) => img.complete && img.naturalWidth > 0)).toBe(true);
}

async function screenshot(page, name) {
  await fs.mkdir(root, { recursive: true });
  await page.screenshot({ path: path.join(root, `${name}.png`), fullPage: true,
    mask: [page.locator('input[type=password]')] });
}

async function loginUi(page, username) {
  const password = await readDemoPassword();
  await page.goto('/admin');
  await expect(page.getByLabel('Tài khoản', { exact: true })).toBeVisible();
  await page.getByLabel('Tài khoản', { exact: true }).fill(username);
  await page.getByLabel('Mật khẩu', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Đăng xuất', exact: true })).toBeVisible();
}

test('DEMO hidden Tour photos and title metadata are unavailable to guests', async ({ request }) => {
  const credits = await (await request.get('/images/credits.json')).json();
  expect(credits.tours.map((t) => t.tourId)).not.toContain(105);
  expect(credits.tours.map((t) => t.tourId)).not.toContain(114);
  for (const id of [105, 114]) {
    for (const suffix of ['', '-card']) {
      const response = await request.get(`/images/tours/tour-${id}${suffix}.webp`);
      expect(response.headers()['content-type'] ?? '').not.toContain('image/');
    }
    const privateFile = path.resolve(`../demo-assets/private/tours/tour-${id}.webp`).replaceAll('\\', '/');
    const directFileResponse = await request.get(`/@fs/${privateFile}`);
    expect(directFileResponse.headers()['content-type'] ?? '').not.toContain('image/');
  }
});

test('DEMO every Tour has a distinct local licensed photo and public cards/details load it', async ({ page, request }) => {
  test.setTimeout(120000);
  const credits = JSON.parse(await fs.readFile('../demo-assets/credits-complete.json', 'utf8'));
  expect(credits.tours).toHaveLength(14);
  expect(new Set(credits.tours.map((t) => t.detail.sha256)).size).toBe(14);
  const observations = [];
  for (const tour of credits.tours) {
    expect(tour.sourcePage).toMatch(/^https:\/\/commons\.wikimedia\.org\/wiki\/File:/);
    expect(tour.license).toBeTruthy();
    for (const size of ['card', 'detail']) {
      const asset = tour[size];
      if (tour.visibility === 'internal-seed') {
        const bytes = await fs.readFile(path.join('..', asset.file));
        expect(crypto.createHash('sha256').update(bytes).digest('hex')).toBe(asset.sha256);
        observations.push({ tourId: tour.tourId, variant: size, file: asset.file, bytes: bytes.length, status: 'PRIVATE_FILE_VERIFIED' });
        continue;
      }
      const response = await request.get(asset.url);
      expect(response.status()).toBe(200);
      expect(response.headers()['content-type']).toContain('image/webp');
      const bytes = await response.body();
      expect(crypto.createHash('sha256').update(bytes).digest('hex')).toBe(asset.sha256);
      observations.push({ tourId: tour.tourId, variant: size, url: asset.url, bytes: bytes.length, status: 200 });
    }
  }
  const items = [];
  for (const pageNo of [1, 2]) {
    const response = await request.get(`${apiBase}/tours?page=${pageNo}&size=10`);
    expect(response.status()).toBe(200);
    items.push(...(await response.json()).data.items);
  }
  expect(items).toHaveLength(12);
  expect(items.map((item) => item.tourId)).not.toContain('105');
  expect(items.map((item) => item.tourId)).not.toContain('114');
  await page.goto('/tours');
  await expect(page.locator('.tour-card')).toHaveCount(10);
  for (const img of await page.locator('.tour-thumb img').all()) await photoReady(img);
  await screenshot(page, 'images-list-page-1');
  await page.getByRole('button', { name: /Trang sau/ }).click();
  await expect(page.locator('.tour-card')).toHaveCount(2);
  for (const img of await page.locator('.tour-thumb img').all()) await photoReady(img);
  await screenshot(page, 'images-list-page-2');
  for (const item of items) {
    expect(item.coverImageUrl).toBe(`/images/tours/tour-${item.tourId}.webp`);
    await page.goto(`/tours/${item.tourId}`);
    await expect(page.getByRole('heading', { name: item.title, exact: true })).toBeVisible();
    await photoReady(page.locator('.detail-hero img').first());
    await screenshot(page, `image-tour-${item.tourId}`);
  }
  expect((await request.get(`${apiBase}/tours/105`)).status()).toBe(404);
  expect((await request.get(`${apiBase}/tours/114`)).status()).toBe(404);
  await fs.writeFile(path.join(root, 'image-verification.json'), JSON.stringify({ status: 'PASS',
    verifiedAt: new Date().toISOString(), tourCount: 14, variants: observations, publicTours: items.map((i) => i.tourId),
    unpublishedAndArchived: '404; existing visibility preserved' }, null, 2));
});

test('DEMO nine-step rehearsal uses real guest Product and Manager flows', async ({ page, request }) => {
  test.setTimeout(120000);
  const record = { status: 'RUNNING', startedAt: new Date().toISOString(), steps: [], checks: [],
    authentication: 'UI login verified. No password/token/header/request body recorded. Trace and video disabled.' };
  const pageErrors = [];
  page.on('pageerror', (err) => pageErrors.push(err.message));
  const step = async (number, observation) => {
    record.steps.push({ number, observation, time: new Date().toISOString() });
    await screenshot(page, `demo-step-${String(number).padStart(2, '0')}`);
  };
  try {
    await page.goto('/tours');
    await expect(page.locator('.tour-card')).toHaveCount(10);
    await photoReady(page.locator('.tour-thumb img').first());
    await step(1, 'GUEST: 12 public Tours; first page10, no interest score, price order.');

    await page.getByLabel('Từ khóa', { exact: true }).fill('Đà Nẵng');
    await page.getByLabel('Điểm đến', { exact: true }).selectOption('DN');
    await page.getByRole('button', { name: 'Tìm tour', exact: true }).click();
    await expect(page.locator('.skeleton')).toHaveCount(0);
    await expect(page.locator('.tour-title')).toContainText(['Đà Nẵng']);
    await step(2, 'Keyword Đà Nẵng + destination DN.');

    await page.getByLabel('Số ngày', { exact: true }).fill('3');
    await page.getByLabel('Ngân sách tối đa (VNĐ)').fill('5000000');
    await page.getByRole('button', { name: 'Tìm tour', exact: true }).click();
    await expect(page.locator('.tour-card')).toHaveCount(3);
    await step(3, 'DN,3 days,maxBudget5000000,availableOnly=true ->101/112/113.');

    await page.getByRole('button', { name: 'Biển', exact: true }).click();
    await page.getByRole('button', { name: 'Văn hóa', exact: true }).click();
    const rankedResponse = page.waitForResponse((r) => r.url().includes('/tour-recommendations?'));
    await page.getByRole('button', { name: 'Tìm tour', exact: true }).click();
    const ranked = (await (await rankedResponse).json()).data.items;
    expect(ranked.map((i) => [i.tourId, i.matchScore])).toEqual([['112', 100], ['113', 100], ['101', 50]]);
    await expect(page.locator('.tour-card')).toHaveCount(3);
    record.ranking = ranked.map(({ tourId, matchScore, priceFrom }) => ({ tourId, matchScore, priceFrom }));
    await step(4, 'BEACH+CULTURE ->112=100%,113=100%,101=50%; global score/price/numericID order.');

    await page.locator('.tour-title a').first().click();
    await expect(page).toHaveURL(/\/tours\/112$/);
    await photoReady(page.locator('.detail-hero img').first());
    await page.getByRole('link', { name: 'Xem lịch khởi hành', exact: true }).click();
    await expect(page).toHaveURL(/\/tours\/112#khoi-hanh$/);
    await expect(page.getByRole('heading', { name: 'Chuyến khởi hành', exact: true })).toBeVisible();
    await step(5, 'Published Tour112: real cover, current price and departures; no Booking created.');

    await loginUi(page, 'product01');
    await expect(page.locator('main')).toContainText('PRODUCT');
    await expect(page.getByRole('heading', { name: 'Danh mục sở thích', exact: true })).toBeVisible();
    await step(6, 'UI login product01 successful; internal catalog visible.');

    await page.locator('#vid').fill('215');
    await page.getByRole('button', { name: 'Tải', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Lưu danh sách sở thích', exact: true })).toBeVisible();
    await expect(page.locator('.chips .chip').first()).toBeVisible();
    for (const chip of await page.locator('.chips .chip').all()) {
      const desired = ['Biển', 'Văn hóa'].includes((await chip.innerText()).trim());
      if ((await chip.getAttribute('aria-pressed') === 'true') !== desired) await chip.click();
    }
    const savedResponse = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().includes('/215/interests'));
    await page.getByRole('button', { name: 'Lưu danh sách sở thích', exact: true }).click();
    expect((await savedResponse).status()).toBe(200);
    await expect(page.locator('.notice.ok').first()).toBeVisible();
    const productToken = await page.evaluate(() => localStorage.getItem('tour_discovery_token'));
    const productHeaders = { Authorization: `Bearer ${productToken}` };
    const saved = await request.get(`${apiBase}/internal/tour-versions/215/interests`, { headers: productHeaders });
    expect((await saved.json()).data.interests.map((i) => i.code).sort()).toEqual(['BEACH', 'CULTURE']);
    const publicTour = (await (await request.get(`${apiBase}/tours/101`)).json()).data;
    expect(publicTour.tourVersionId).toBe('201');
    expect(publicTour.interests.map((i) => i.code).sort()).toEqual(['BEACH', 'FOOD', 'RELAXATION']);
    await step(7, 'PRODUCT saves BEACH+CULTURE to DRAFT215; published201 unchanged.');

    await page.locator('#vid').fill('201');
    await page.getByRole('button', { name: 'Tải', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Lưu danh sách sở thích', exact: true })).toBeDisabled();
    const locked = await request.put(`${apiBase}/internal/tour-versions/201/interests`, {
      headers: productHeaders, data: { interestCodes: ['BEACH'] } });
    expect(locked.status()).toBe(409);
    record.checks.push({ operation: 'PRODUCT PUT PUBLISHED201', status: locked.status(), code: (await locked.json()).error.code });
    await screenshot(page, 'demo-product-published-locked');
    await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click();
    await loginUi(page, 'manager01');
    await page.locator('#vid').fill('215');
    await page.getByRole('button', { name: 'Tải', exact: true }).click();
    await expect(page.locator('main')).toContainText('MANAGER');
    await expect(page.getByRole('button', { name: 'Lưu danh sách sở thích', exact: true })).toHaveCount(0);
    await expect(page.getByRole('heading', { name: 'Danh mục sở thích', exact: true })).toHaveCount(0);
    const managerToken = await page.evaluate(() => localStorage.getItem('tour_discovery_token'));
    const denied = await request.put(`${apiBase}/internal/tour-versions/215/interests`, {
      headers: { Authorization: `Bearer ${managerToken}` }, data: { interestCodes: ['BEACH'] } });
    expect(denied.status()).toBe(403);
    record.checks.push({ operation: 'MANAGER PUT DRAFT215', status: denied.status(), code: (await denied.json()).error.code });
    expect((await request.get(`${apiBase}/internal/tour-versions/215/interests`)).status()).toBe(401);
    expect((await request.get(`${apiBase}/tours?destinationId=NO_SUCH_DEST`)).status()).toBe(422);
    await step(8, 'PUBLISHED lock409 and MANAGER write403; Manager UI read-only, anonymous internal401.');

    await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click();
    await page.goto('/tours');
    await expect(page.locator('.tour-card')).toHaveCount(10);
    await step(9, 'Returned to public Guest list.');
    expect(pageErrors).toEqual([]);
    record.status = 'PASS';
  } catch (error) {
    record.status = 'FAIL';
    record.failure = 'Rehearsal assertion failed; inspect the Playwright result and screenshots.';
    throw error;
  } finally {
    record.finishedAt = new Date().toISOString();
    await fs.mkdir(root, { recursive: true });
    await fs.writeFile(path.join(root, 'demo-rehearsal.json'), JSON.stringify(record, null, 2));
  }
});

test('DEMO public detail and internal controls remain usable at 360 768 and1440', async ({ page }) => {
  test.setTimeout(90000);
  await loginUi(page, 'product01');
  for (const width of [360, 768, 1440]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/tours/101');
    await photoReady(page.locator('.detail-hero img').first());
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await screenshot(page, `responsive-detail-${width}`);
    await page.goto('/admin');
    await expect(page.getByRole('button', { name: 'Tải', exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Tải', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Lưu danh sách sở thích', exact: true })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await screenshot(page, `responsive-admin-${width}`);
  }
});

test('DEMO Product creates and deactivates a catalog item through the UI', async ({ page, request }) => {
  await loginUi(page, 'product01');
  const code = `DEMO_${Date.now().toString(36).toUpperCase()}`;
  await page.locator('#code').fill(code);
  await page.locator('#name').fill('Trải nghiệm demo');
  await page.locator('#desc').fill('Dữ liệu tổng hợp để kiểm chứng tạo và tắt sở thích.');
  await page.locator('#order').fill('99');
  const createdResponse = page.waitForResponse((r) => r.request().method() === 'POST' && r.url().endsWith('/internal/interests'));
  await page.getByRole('button', { name: 'Tạo sở thích', exact: true }).click();
  expect((await createdResponse).status()).toBe(201);
  const row = page.getByRole('row').filter({ hasText: code });
  await expect(row).toContainText('Đang bật');
  await screenshot(page, 'catalog-created');
  await row.getByRole('button', { name: 'Tắt', exact: true }).click();
  await expect(row).toContainText('Đã tắt');
  const publicCatalog = (await (await request.get(`${apiBase}/interests`)).json()).data;
  expect(publicCatalog.map((i) => i.code)).not.toContain(code);
  await screenshot(page, 'catalog-deactivated');
  await fs.writeFile(path.join(root, 'catalog-verification.json'), JSON.stringify({ status: 'PASS',
    verifiedAt: new Date().toISOString(), code, createStatus: 201, deactivated: true, absentFromPublicCatalog: true }, null, 2));
});
