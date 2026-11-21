import { test, expect } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';

const evidenceRoot = process.env.MODULE_A_EVIDENCE_DIR ?? path.resolve('test-results/module-a');
const responseJobs = new WeakMap();
const evidenceRecords = new WeakMap();

function gate() {
  let release;
  const promise = new Promise((resolve) => { release = resolve; });
  return { promise, release };
}

async function flushRendering(page) {
  await page.evaluate(() => new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve))));
}

async function pageReady(page) {
  await page.goto('/tours');
  await expect(page.locator('.tour-card').first()).toBeVisible();
  await expect(page.locator('.skeleton')).toHaveCount(0);
  await expect(page.locator('.chips .chip').first()).toBeVisible();
}

async function tabTo(page, selector) {
  for (let index = 0; index < 60; index += 1) {
    if (await page.locator(selector).evaluateAll((elements) => elements.includes(document.activeElement))) return;
    await page.keyboard.press('Tab');
  }
  throw new Error(`Tab did not reach ${selector}`);
}

async function assertUnobscured(locator) {
  await locator.evaluate((element) => element.scrollIntoView({ block: 'center' }));
  const observation = await locator.evaluate((element) => {
    const rect = element.getBoundingClientRect();
    const x = rect.left + rect.width / 2;
    const y = rect.top + rect.height / 2;
    const hit = document.elementFromPoint(x, y);
    return {
      visible: x >= 0 && x < innerWidth && y >= 58 && y < innerHeight,
      unobscured: hit === element || element.contains(hit),
      rect: { x: rect.x, y: rect.y, width: rect.width, height: rect.height },
      hit: hit?.outerHTML.slice(0, 350),
    };
  });
  const observations = evidenceRecords.get(locator.page()).record.observations;
  observations.hitTests ??= [];
  observations.hitTests.push(observation);
  expect(observation, `Control hit test: ${JSON.stringify(observation)}`).toMatchObject({
    visible: true,
    unobscured: true,
  });
  return observation;
}

test.beforeEach(async ({ page, browser, context }, testInfo) => {
  const tc = testInfo.title.match(/TC\d+/)[0];
  const scenario = testInfo.title.replace(/[^a-zA-Z0-9-]+/g, '-');
  const directory = path.join(evidenceRoot, tc, scenario);
  await fs.mkdir(directory, { recursive: true });
  const sourceFiles = [
    'src/pages/SearchPage.jsx', 'src/components/FilterPanel.jsx',
    'src/components/TourCard.jsx', 'src/api/client.js', 'src/styles/app.css',
    'src/pages/TourDetailPage.jsx', 'tests/module-a-browser.spec.js', 'playwright.config.js',
    'package.json', 'package-lock.json',
  ];
  const sources = {};
  for (const sourceFile of sourceFiles) {
    sources[sourceFile] = crypto.createHash('sha256').update(await fs.readFile(sourceFile)).digest('hex');
  }
  const record = {
    tc, title: testInfo.title, startedAt: new Date().toISOString(),
    browser: browser.version(), os: process.platform, node: process.version,
    baseUrl: testInfo.project.use.baseURL, sourceSha256: sources,
    viewport: page.viewportSize(), network: [], console: [], observations: {},
    coreData: 'Real running backend. Transport gates/faults in TC31/32; TC34 changes only coverImageUrl to null or aborts a real local image. TC45 has no interception.',
    cleanInstallEvidence: process.env.MODULE_A_INSTALL_MANIFEST ?? null,
  };
  evidenceRecords.set(page, { record, directory });
  responseJobs.set(page, []);
  page.on('response', (response) => {
    if (!response.url().includes('/api/v1/')) return;
    const job = (async () => {
      let body;
      try { body = await response.text(); } catch { body = '[response body unavailable after browser transport failure]'; }
      record.network.push({ time: new Date().toISOString(), method: response.request().method(), url: response.url(), status: response.status(), body });
    })();
    responseJobs.get(page).push(job);
  });
  page.on('requestfailed', (request) => {
    if (request.url().includes('/api/v1/')) record.network.push({ time: new Date().toISOString(), url: request.url(), failure: request.failure() });
  });
  page.on('console', (message) => { if (message.type() === 'error') record.console.push(message.text()); });
  page.on('pageerror', (error) => record.console.push(error.message));
  await context.tracing.start({ screenshots: true, snapshots: true, sources: true });
});

test.afterEach(async ({ page, context }, testInfo) => {
  const { record, directory } = evidenceRecords.get(page);
  record.finishedAt = new Date().toISOString();
  record.status = testInfo.status === 'passed' ? 'PASS' : 'FAIL';
  record.errors = testInfo.errors.map((error) => error.message);
  record.viewport = page.viewportSize();
  record.observations.renderedTitles = await page.locator('.tour-title').allTextContents().catch(() => []);
  record.observations.error = await page.locator('.notice.error').allTextContents().catch(() => []);
  await page.screenshot({ path: path.join(directory, 'viewport.png') }).catch(() => {});
  await page.screenshot({ path: path.join(directory, 'screenshot.png'), fullPage: true }).catch(() => {});
  await context.tracing.stop({ path: path.join(directory, 'trace.zip') });
  await Promise.allSettled(responseJobs.get(page));
  await fs.writeFile(path.join(directory, 'result.json'), JSON.stringify(record, null, 2));
});

test('TC45 valid integer budget supports real filter rank and detail', async ({ page }) => {
  await pageReady(page);
  await page.getByLabel('Điểm đến', { exact: true }).selectOption('DN');
  await page.getByLabel('Số ngày', { exact: true }).fill('3');
  const budget = page.getByLabel('Ngân sách tối đa (VNĐ)');
  const validity = [];
  evidenceRecords.get(page).record.observations.budgetValidity = validity;
  for (const value of ['5000000', '1', '4200000', '5000001']) {
    await budget.fill(value);
    const actual = await budget.evaluate((element) => ({ valid: element.validity.valid, stepMismatch: element.validity.stepMismatch }));
    validity.push({ value, ...actual });
    expect(actual, `${value} is valid integer VND`).toEqual({ valid: true, stepMismatch: false });
  }
  for (const value of ['0', '-1', '1.5']) {
    await budget.fill(value);
    const valid = await budget.evaluate((element) => element.validity.valid);
    validity.push({ value, valid });
    expect(valid, `${value} must be rejected`).toBe(false);
  }
  await budget.fill('5000000');
  await page.getByRole('button', { name: 'Biển', exact: true }).click();
  await page.getByRole('button', { name: 'Văn hóa', exact: true }).click();
  const responsePromise = page.waitForResponse((response) => response.url().includes('/tour-recommendations?'));
  await page.getByRole('button', { name: 'Tìm tour', exact: true }).click();
  const response = await responsePromise;
  expect(response.status()).toBe(200);
  const payload = await response.json();
  evidenceRecords.get(page).record.observations.rankedItems = payload.data.items;
  expect(payload.data.items.map((item) => [item.tourId, item.matchScore])).toEqual([
    ['112', 100], ['113', 100], ['101', 50],
  ]);
  expect(payload.meta).toMatchObject({ personalized: true, sort: 'RELEVANCE' });
  await expect(page.locator('.tour-card')).toHaveCount(3);
  await expect(page.locator('.tour-card .score-badge')).toHaveText(['Trùng 100%', 'Trùng 100%', 'Trùng 50%']);
  const detailPromise = page.waitForResponse((response) => response.url().endsWith('/tours/112'));
  await page.locator('.tour-title a').first().click();
  const detail = await (await detailPromise).json();
  expect(detail.data.tourId).toBe('112');
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Đà Nẵng - Hội An trọn gói');
  await expect(page.getByRole('heading', { name: 'Chuyến khởi hành' })).toBeVisible();
});

test('TC31 stale success cannot replace newer reset results', async ({ page }) => {
  await pageReady(page);
  const pending = gate();
  const fetched = gate();
  await page.route('**/api/v1/tours?**', async (route) => {
    if (new URL(route.request().url()).searchParams.get('keyword') !== 'Hà Nội') return route.continue();
    const response = await route.fetch();
    fetched.release();
    await pending.promise;
    await route.fulfill({ response });
  });
  await page.getByLabel('Từ khóa', { exact: true }).fill('Hà Nội');
  await page.getByRole('button', { name: 'Tìm tour', exact: true }).click();
  await fetched.promise;
  await page.getByRole('button', { name: 'Xóa lọc', exact: true }).click();
  await expect(page.locator('.tour-card')).toHaveCount(10);
  const oldFinished = page.waitForEvent('requestfinished', { predicate: (request) => new URL(request.url()).searchParams.get('keyword') === 'Hà Nội' });
  pending.release();
  await oldFinished;
  await flushRendering(page);
  await expect(page.locator('.tour-card')).toHaveCount(10);
});

test('TC31 stale error cannot erase a newer successful reset', async ({ page }) => {
  await pageReady(page);
  const pending = gate();
  const fetched = gate();
  await page.route('**/api/v1/tours?**', async (route) => {
    if (new URL(route.request().url()).searchParams.get('keyword') !== 'Hà Nội') return route.continue();
    await route.fetch();
    fetched.release();
    await pending.promise;
    await route.abort('timedout');
  });
  await page.getByLabel('Từ khóa', { exact: true }).fill('Hà Nội');
  await page.getByRole('button', { name: 'Tìm tour', exact: true }).click();
  await fetched.promise;
  await page.getByRole('button', { name: 'Xóa lọc', exact: true }).click();
  await expect(page.locator('.tour-card')).toHaveCount(10);
  const failed = page.waitForEvent('requestfailed', { predicate: (request) => request.url().includes('keyword=') });
  pending.release();
  await failed;
  await flushRendering(page);
  await expect(page.locator('.notice.error')).toHaveCount(0);
  await expect(page.locator('.tour-card')).toHaveCount(10);
});

test('TC31 old completion cannot stop newer loading state', async ({ page }) => {
  await pageReady(page);
  const old = gate();
  const latest = gate();
  const oldFetched = gate();
  const latestFetched = gate();
  await page.route('**/api/v1/tours?**', async (route) => {
    const response = await route.fetch();
    const isOld = new URL(route.request().url()).searchParams.has('keyword');
    (isOld ? oldFetched : latestFetched).release();
    await (isOld ? old : latest).promise;
    await route.fulfill({ response });
  });
  await page.getByLabel('Từ khóa', { exact: true }).fill('Hà Nội');
  await page.getByRole('button', { name: 'Tìm tour', exact: true }).click();
  await oldFetched.promise;
  await page.getByRole('button', { name: 'Xóa lọc', exact: true }).click();
  await latestFetched.promise;
  const oldResponse = page.waitForResponse((response) => new URL(response.url()).searchParams.get('keyword') === 'Hà Nội');
  old.release();
  await oldResponse;
  await flushRendering(page);
  try {
    await expect(page.locator('.skeleton')).toHaveCount(3);
    await expect(page.getByRole('button', { name: 'Đang tìm…' })).toBeDisabled();
  } finally { latest.release(); }
  await expect(page.locator('.tour-card')).toHaveCount(10);
});

test('TC32 failed applied filters can retry despite unapplied form edits', async ({ page }) => {
  await pageReady(page);
  let failedOnce = false;
  const requests = [];
  await page.route('**/api/v1/tours?**', async (route) => {
    const requestUrl = new URL(route.request().url());
    if (requestUrl.searchParams.get('keyword') !== 'Hà Nội') return route.continue();
    requests.push(requestUrl.search);
    const response = await route.fetch();
    if (!failedOnce) { failedOnce = true; return route.abort('timedout'); }
    return route.fulfill({ response });
  });
  await page.getByLabel('Từ khóa', { exact: true }).fill('Hà Nội');
  await page.getByLabel('Số tour mỗi trang').selectOption('3');
  await page.getByRole('button', { name: 'Tìm tour', exact: true }).click();
  await expect(page.locator('.notice.error')).toBeVisible();
  await page.getByLabel('Từ khóa', { exact: true }).fill('Đà Nẵng');
  await expect(page.getByRole('button', { name: 'Thử lại', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại', exact: true }).click();
  await expect(page.locator('.notice.error')).toHaveCount(0);
  await expect(page.locator('.tour-card')).toHaveCount(1);
  await expect(page.locator('.tour-title')).toHaveText('Hà Nội city tour');
  expect(requests).toHaveLength(2);
  expect(requests[1]).toBe(requests[0]);
  evidenceRecords.get(page).record.observations.retryQueryStrings = requests;
});

test('TC32 catalog timeout can retry missing filter choices', async ({ page }) => {
  const pending = gate();
  let failCatalog = true;
  await page.route('**/api/v1/interests', async (route) => {
    const response = await route.fetch();
    if (failCatalog) {
      await pending.promise;
      return route.abort('timedout');
    }
    return route.fulfill({ response });
  });
  await page.goto('/tours');
  await expect(page.locator('.tour-card')).toHaveCount(10);
  pending.release();
  await expect(page.locator('.notice.error')).toBeVisible();
  await expect(page.locator('.chips .chip')).toHaveCount(0);
  failCatalog = false;
  await page.getByRole('button', { name: 'Thử lại', exact: true }).click();
  await expect(page.locator('.chips .chip')).toHaveCount(10);
  await expect(page.getByLabel('Điểm đến', { exact: true }).locator('option')).toHaveCount(10);
  await expect(page.locator('.notice.error')).toHaveCount(0);
  await expect(page.locator('.tour-card')).toHaveCount(10);
});

test('TC33 empty filters can reset to real results', async ({ page }) => {
  await pageReady(page);
  await page.getByLabel('Số ngày', { exact: true }).fill('999');
  await page.getByRole('button', { name: 'Tìm tour', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Không có tour nào đạt điều kiện' })).toBeVisible();
  await expect(page.locator('.tour-card')).toHaveCount(0);
  await page.getByRole('button', { name: 'Xóa lọc', exact: true }).click();
  await expect(page.getByLabel('Số ngày', { exact: true })).toHaveValue('');
  await expect(page.locator('.tour-card')).toHaveCount(10);
  await expect(page.locator('.empty')).toHaveCount(0);
});

test('TC34 null image fallback preserves card geometry', async ({ page }) => {
  // Every demo Tour now has a cover; isolate the missing-media branch without
  // changing search results, prices, scoring or the database seed.
  await page.route('**/api/v1/tours?**', async (route) => {
    const response = await route.fetch();
    const payload = await response.json();
    for (const item of payload.data.items) {
      if (item.tourId === '111') item.coverImageUrl = null;
    }
    await route.fulfill({ response, json: payload });
  });
  await pageReady(page);
  const card = page.locator('.tour-card').filter({ has: page.locator('.tour-title', { hasText: 'Hà Nội city tour' }) });
  await expect(card.locator('.tour-thumb')).toHaveText('Chưa có ảnh đại diện');
  await expect(card.locator('img')).toHaveCount(0);
  const before = await card.boundingBox();
  await page.getByLabel('Từ khóa', { exact: true }).fill('Hà Nội');
  await page.getByRole('button', { name: 'Tìm tour', exact: true }).click();
  await expect(page.locator('.tour-card')).toHaveCount(1);
  const after = await card.boundingBox();
  expect(after.width).toBe(before.width);
  expect(after.height).toBe(before.height);
  expect((await card.locator('.tour-thumb').boundingBox()).height).toBeGreaterThanOrEqual(150);
  evidenceRecords.get(page).record.observations.geometry = { before, after };
});

test('TC34 unavailable real cover image falls back without changing geometry', async ({ page }) => {
  const pending = gate();
  await page.route('**/images/tours/tour-106*.webp', async (route) => {
    await pending.promise;
    await route.abort('failed');
  });
  await pageReady(page);
  await page.getByLabel('Từ khóa', { exact: true }).fill('Hội An');
  await page.getByRole('button', { name: 'Tìm tour', exact: true }).click();
  const card = page.locator('.tour-card').filter({ has: page.locator('.tour-title', { hasText: 'Hội An phố cổ và ẩm thực' }) });
  await expect(card.locator('img')).toBeVisible();
  const before = await card.boundingBox();
  pending.release();
  await expect(card.locator('.tour-thumb')).toHaveText('Chưa có ảnh đại diện');
  await expect(card.locator('img')).toHaveCount(0);
  const after = await card.boundingBox();
  expect(after.width).toBe(before.width);
  expect(after.height).toBe(before.height);
  evidenceRecords.get(page).record.observations.geometry = { before, after };
});

for (const width of [360, 768, 1440]) {
  test(`TC35 viewport ${width} keeps filters and card links unobscured`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await pageReady(page);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'No horizontal overflow').toBe(true);
    const observations = [];
    observations.push(await assertUnobscured(page.getByRole('button', { name: 'Tìm tour', exact: true })));
    observations.push(await assertUnobscured(page.locator('.tour-title a').first()));
    observations.push(await assertUnobscured(page.locator('.tour-title a').last()));
    evidenceRecords.get(page).record.observations.hitTests = observations;
  });
}

test('TC36 keyboard Tab Enter exposes selected interest and submits', async ({ page }) => {
  await pageReady(page);
  await tabTo(page, '#keyword');
  await page.keyboard.type('Hà Nội');
  await tabTo(page, '.chip:first-child');
  const chip = page.locator('.chip').first();
  await expect(chip).toBeFocused();
  const focusStyle = await chip.evaluate((element) => ({ outline: getComputedStyle(element).outlineStyle, width: getComputedStyle(element).outlineWidth }));
  expect(focusStyle.outline).not.toBe('none');
  expect(focusStyle.width).not.toBe('0px');
  await page.keyboard.press('Enter');
  await expect(chip).toHaveAttribute('aria-pressed', 'true');
  await page.keyboard.press('Enter');
  await expect(chip).toHaveAttribute('aria-pressed', 'false');
  await page.keyboard.press('Enter');
  await tabTo(page, 'button[type="submit"]');
  await expect(page.locator('button[type="submit"]')).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page.locator('.tour-card')).toHaveCount(1);
  await expect(page.locator('.score-badge')).toHaveText('Trùng 0%');
  await tabTo(page, '.tour-title a');
  await expect(page.locator('.tour-title a')).toBeFocused();
  await assertUnobscured(page.locator('.tour-title a'));
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/\/tours\/111$/);
});
