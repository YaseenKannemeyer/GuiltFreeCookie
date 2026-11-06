// Run against Vite: PLAYWRIGHT_MODULE=/path/to/playwright node frontend/tests/mobile.browser.cjs
const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const origin = process.env.TEST_ORIGIN || 'http://127.0.0.1:5173';
const cookie = {
  cookieId: 'mobile-cookie', slug: 'double-chocolate-chunk', name: 'Double Chocolate Chunk',
  category: 'HIGH_PROTEIN', image: '/img/cookies/double-chocolate-chunk.webp',
  fromPrice: 34, featured: true, stockStatus: 'IN_STOCK', reviewCount: 0,
  longDescription: 'Deep cocoa and dark chocolate chunks with a soft centre.',
  ingredients: 'Almond flour, cocoa', allergies: 'Almonds', images: [], related: [],
  variants: [{ variantId: 'single', label: 'Single', unitsPerPack: 1, price: 34, pricePerCookie: 34, available: true }],
};
const cart = {
  cartId: 'mobile-cart', status: 'ACTIVE', itemCount: 1, cookieCount: 1,
  subtotal: 34, total: 34, discountTotal: 0, canCheckout: true,
  items: [{ cartItemId: 'line', cookieId: cookie.cookieId, slug: cookie.slug, name: cookie.name,
    image: cookie.image, variantLabel: 'Single', unitsPerPack: 1, quantity: 1, maxQuantity: 10, lineTotal: 34 }],
};
async function mockApi(route) {
  const path = new URL(route.request().url()).pathname;
  let data = {};
  if (path.endsWith('/site/info')) data = { social: {}, stats: {} };
  else if (path.endsWith('/cookies/categories')) data = [{ category: 'HIGH_PROTEIN', label: 'High protein', count: 1 }];
  else if (path.endsWith('/cookies')) data = { items: [cookie], page: 0, totalPages: 1, totalItems: 1 };
  else if (path.endsWith('/reviews/featured')) data = [];
  else if (path.endsWith('/reviews')) data = { summary: { reviewCount: 0, histogram: {} }, page: { items: [], totalPages: 0, totalItems: 0 } };
  else if (path.includes('/cookies/')) data = cookie;
  else if (path.includes('/carts')) data = cart;
  await route.fulfill({ json: data });
}
(async () => {
  const browser = await chromium.launch({ headless: true, ...(process.env.CHROME_PATH ? { executablePath: process.env.CHROME_PATH } : {}) });
  const errors = [];
  try {
    for (const [width, height] of [[320, 740], [390, 844], [768, 1024], [844, 390], [1440, 900]]) {
      const context = await browser.newContext({ viewport: { width, height }, hasTouch: width < 1024, reducedMotion: 'reduce' });
      await context.addInitScript(() => localStorage.setItem('gfc_cart_id', 'mobile-cart'));
      await context.route('**/guiltFreeCookie/**', mockApi);
      const page = await context.newPage();
      page.on('pageerror', error => errors.push(`${width}: ${error.message}`));
      await page.goto(origin + '/products');
      await page.getByRole('heading', { name: cookie.name }).waitFor();
      const extra = page.locator('#extra-filters');
      if (width < 640) {
        assert.equal(await extra.isVisible(), false);
        const toggle = page.getByRole('button', { name: 'Filter & sort' });
        await toggle.tap();
        await page.getByLabel('Min price (R)').fill('20');
        await page.waitForURL('**/*minPrice=20*');
        await page.getByRole('button', { name: 'Filter & sort (1)' }).tap();
        assert.equal(await extra.isVisible(), false);
        await page.getByRole('button', { name: 'Clear filters' }).tap();
        await page.waitForURL(url => !url.searchParams.has('minPrice'));
      } else assert.equal(await extra.isVisible(), true);
      if (width < 1024) {
        const opener = page.getByRole('button', { name: 'Open menu' });
        const rect = await opener.boundingBox();
        assert.ok(rect.x >= 0 && rect.x + rect.width <= width && rect.width >= 44);
        await opener.tap();
        const dialog = page.getByRole('dialog', { name: 'Menu' });
        await dialog.waitFor();
        assert.equal(await page.evaluate(() => document.getElementById('app').inert), true);
        assert.equal(await page.getByRole('button', { name: 'Close menu' }).evaluate(el => el === document.activeElement), true);
        await page.keyboard.press('Shift+Tab');
        assert.equal(await dialog.getByRole('link', { name: 'Shop cookies' }).evaluate(el => el === document.activeElement), true);
        await page.keyboard.press('Tab');
        await page.keyboard.press('Escape');
        await dialog.waitFor({ state: 'hidden' });
        assert.equal(await opener.evaluate(el => el === document.activeElement), true);
        assert.equal(await page.evaluate(() => document.getElementById('app').inert), false);
        await opener.tap();
        await dialog.getByRole('button', { name: 'Search cookies' }).tap();
        await dialog.waitFor({ state: 'hidden' });
        await opener.tap();
        await page.setViewportSize({ width: 1280, height: 900 });
        await dialog.waitFor({ state: 'hidden' });
        await page.waitForFunction(() => document.body.style.overflow === '' && !document.getElementById('app').inert);
        assert.equal(await page.evaluate(() => document.body.style.overflow), '');
        await page.setViewportSize({ width, height });
      }
      for (const path of ['/', '/products', '/product/' + cookie.slug, '/cart', '/checkout/you', '/about', '/contact']) {
        await page.goto(origin + path);
        await page.locator('main h1').first().waitFor();
        await page.waitForTimeout(200);
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1), false, `Overflow: ${width} ${path}`);
        if (path.startsWith('/product/')) {
          const button = page.getByRole('button', { name: 'Add to cart' });
          await button.waitFor();
          const fits = await button.evaluate(el => el.scrollWidth <= el.clientWidth + 1);
          assert.ok(fits, `Purchase button clipped at ${width}px`);
        }
      }
      await context.close();
    }
    assert.deepEqual(errors, []);
    console.log('PASS: 7 storefront routes at 5 viewport sizes; touch menu, focus containment/restoration, resize cleanup, filter state, purchase button fit, and no page errors. API responses are fixtures.');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
