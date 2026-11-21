import { test, expect } from '@playwright/test';

const tourPath = '/tours/111';
const tourApi = '**/api/v1/tours/111';
const failedCover = '/test-fixtures/unavailable-tour-cover.webp';
const sampleImage = 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7';

async function withCover(page, url) {
  await page.route(tourApi, async (route) => {
    const response = await route.fetch();
    const payload = await response.json();
    payload.data.coverImageUrl = url;
    payload.data.media = [];
    await route.fulfill({ response, json: payload });
  });
}

test('tour detail reserves a labeled image area when no cover exists', async ({ page }) => {
  await withCover(page, null);
  await page.goto(tourPath);

  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Hà Nội city tour');
  const hero = page.locator('.detail-hero');
  await expect(hero.getByRole('img', { name: 'Chưa có ảnh đại diện: Hà Nội city tour' })).toBeVisible();
  expect((await hero.boundingBox()).height).toBeGreaterThanOrEqual(150);
});

test('tour detail keeps cover geometry on image failure without retrying the broken URL', async ({ page }) => {
  let releaseImage;
  const imageGate = new Promise((resolve) => { releaseImage = resolve; });
  let imageRequests = 0;
  await page.route(`**${failedCover}`, async (route) => {
    imageRequests += 1;
    await imageGate;
    await route.abort('failed');
  });
  await withCover(page, failedCover);
  await page.goto(tourPath, { waitUntil: 'domcontentloaded' });

  const hero = page.locator('.detail-hero');
  let original;
  try {
    await expect(hero.locator('img')).toBeVisible();
    original = await hero.boundingBox();
  } finally {
    releaseImage();
  }
  await expect(hero.getByRole('img', { name: 'Chưa có ảnh đại diện: Hà Nội city tour' })).toBeVisible();
  await expect(hero.locator('img')).toHaveCount(0);
  expect(await hero.boundingBox()).toEqual(original);
  expect(imageRequests).toBe(1);
});

test('tour detail presents the cover once and shows captioned gallery photos', async ({ page }) => {
  await page.route('**/api/v1/tours/101', async (route) => {
    const response = await route.fetch();
    const payload = await response.json();
    payload.data.coverImageUrl = sampleImage;
    payload.data.media = [
      { mediaId: 'cover', mediaType: 'IMAGE', mediaUrl: sampleImage, altText: 'Bãi Mỹ Khê', cover: true },
      { mediaId: 'market', mediaType: 'IMAGE', mediaUrl: `${sampleImage}#market`, altText: 'Chợ Hàn', caption: 'Khám phá Chợ Hàn' },
      { mediaId: 'video', mediaType: 'VIDEO', mediaUrl: '/not-an-image.mp4', altText: 'Video hành trình' },
    ];
    await route.fulfill({ response, json: payload });
  });
  await page.goto('/tours/101');

  await expect(page.getByRole('img', { name: 'Bãi Mỹ Khê', exact: true })).toHaveCount(1);
  const gallery = page.getByRole('region', { name: 'Hình ảnh hành trình' });
  const photo = gallery.getByRole('img', { name: 'Chợ Hàn', exact: true });
  await photo.scrollIntoViewIfNeeded();
  await expect.poll(() => photo.evaluate((image) => image.naturalWidth)).toBeGreaterThan(0);
  await expect(gallery.getByText('Khám phá Chợ Hàn', { exact: true })).toBeVisible();
  await expect(gallery.getByRole('img')).toHaveCount(1);
});

test('tour cards display destination names from the destination catalog', async ({ page }) => {
  await page.goto('/tours');
  const card = page.locator('.tour-card').filter({ has: page.getByRole('link', { name: 'Hà Nội city tour' }) });
  await expect(card.locator('.tour-meta')).toContainText('Hà Nội');
});
