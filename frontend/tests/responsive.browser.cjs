// Start Vite, then: PLAYWRIGHT_MODULE=/path/to/playwright npm run test:responsive
// BROWSER=chromium (default), firefox, or webkit; TEST_ORIGIN overrides localhost:5173.
// Firefox uses mouse/keyboard at each viewport; touch swipes require Chromium CDP.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const playwright = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const engine = process.env.BROWSER || 'chromium';
assert.ok(['chromium', 'firefox', 'webkit'].includes(engine), 'BROWSER must be chromium, firefox, or webkit');
const { mockApi, seed, seedData, cookie } = require('./responsive-fixtures.cjs');
const states = require('./responsive-states.cjs');
const interactions = require('./responsive-interactions.cjs');
const origin = process.env.TEST_ORIGIN || 'http://127.0.0.1:5173';
const output = process.env.RESPONSIVE_OUTPUT || path.join(__dirname, '../../target/responsive', engine);
const routes = [
  '/', '/products', `/product/${cookie.slug}`, '/about', '/contact', '/cart',
  '/checkout/you', '/checkout/delivery', '/checkout/pay', '/order/GFC-20261106-1042?token=fixture',
  '/review/GFC-20261106-1042?token=fixture', '/cart/recover/fixture', '/reorder/GFC-20261106-1042?token=fixture', '/checkout/cancelled', '/demo-payment',
  '/login', '/register', '/forgot-password', '/reset-password?token=fixture', '/account', '/account/claim?token=fixture',
  '/email/unsubscribe?e=sam@example.com&t=fixture', '/email/resubscribe?e=sam@example.com&t=fixture', '/email/subscribed?e=sam@example.com&t=fixture',
  '/privacy-policy', '/terms', '/cookie-policy', '/refund-policy', '/missing-page',
  ...['overview', 'cookies', 'users', 'orders', 'orders/GFC-20261106-1042', 'inventory', 'delivery', 'discounts', 'messages', 'reviews', 'email-flows', 'email-previews', 'profile'].map(p => `/admindashboard/${p}`),
];
const sizes = [[320, 740], [390, 844], [768, 1024], [1024, 768], [844, 390], [1280, 800], [1440, 900], [2560, 1440], [3840, 2160]];
async function inspect(page, label, failures) {
  const problems = await page.evaluate(() => {
    const issues = [];
    if (document.documentElement.scrollWidth > innerWidth + 1) issues.push(`page width ${document.documentElement.scrollWidth} > ${innerWidth}`);
    for (const panel of document.querySelectorAll('dialog[open] .modal-panel')) {
      const r = panel.getBoundingClientRect();
      if (r.top < -1 || r.bottom > innerHeight + 1) issues.push('dialog panel exceeds viewport height');
    }
    // Catch clipped content even when an ancestor hides page overflow.
    for (const el of document.querySelectorAll('main button, main .btn, main input, main select, main textarea, dialog[open] button, dialog[open] input, dialog[open] select, dialog[open] textarea')) {
      if (!el.getClientRects().length || el.closest('[aria-hidden="true"], .sr-only')) continue;
      const rect = el.getBoundingClientRect();
      if (rect.width < 1 || getComputedStyle(el).visibility === 'hidden') continue;
      if (el.matches('button, .btn') && el.scrollWidth > el.clientWidth + 2) {
        // A rotating decorative icon can expand scrollWidth without clipping text.
        const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
        let node;
        while ((node = walker.nextNode())) {
          if (!node.textContent.trim() || node.parentElement.closest('[aria-hidden="true"]')) continue;
          const range = document.createRange();
          range.selectNodeContents(node);
          if ([...range.getClientRects()].some(r => r.right > rect.right + 2 || r.left < rect.left - 2)) {
            issues.push(`clipped control: ${el.textContent.trim().slice(0, 65)}`);
            break;
          }
        }
      }
      let scrollable = false;
      for (let parent = el.parentElement; parent; parent = parent.parentElement) {
        if (['auto', 'scroll'].includes(getComputedStyle(parent).overflowX)) scrollable = true;
      }
      if (!scrollable && (rect.right > innerWidth + 1 || rect.left < -1)) issues.push(`offscreen control: ${el.getAttribute('aria-label') || el.textContent.trim().slice(0, 50) || el.tagName}`);
    }
    return [...new Set(issues)];
  });
  for (const issue of problems) failures.push(`${label}: ${issue}`);
}
(async () => {
  fs.mkdirSync(output, { recursive: true });
  const browser = await playwright[engine].launch({ headless: true, ...(engine === 'chromium' && process.env.CHROME_PATH ? { executablePath: process.env.CHROME_PATH } : {}) });
  const failures = [];
  try {
    if (!process.env.INTERACTIONS_ONLY) await states(browser, { origin, inspect, failures });
    for (const [width, height] of sizes) {
      if (process.env.STATES_ONLY || (process.env.INTERACTIONS_ONLY && ![320, 844, 1440].includes(width))) continue;
      const context = await browser.newContext({ viewport: { width, height }, hasTouch: engine !== 'firefox' && width < 1100, reducedMotion: 'reduce' });
      await context.addInitScript(seed, seedData);
      await context.route('**/guiltFreeCookie/**', route => mockApi(route).catch(async e => { failures.push(e.message); await route.fulfill({ status: 500, json: { message: e.message } }); }));
      const page = await context.newPage();
      page.on('pageerror', e => failures.push(`${width}: ${e.message}`));
      for (const route of process.env.INTERACTIONS_ONLY ? [] : routes) {
        await page.goto(origin + route);
        await page.locator('main h1').first().waitFor();
        await page.evaluate(() => document.fonts.ready);
        await page.waitForTimeout(100);
        await inspect(page, `${width}×${height} ${route}`, failures);
        if (process.env.SCREENSHOTS && [320, 768, 1440].includes(width) && ['/', '/cart', '/admindashboard/overview', '/admindashboard/orders', '/admindashboard/users', '/admindashboard/cookies', '/admindashboard/profile'].includes(route)) {
          // Scroll through revealed sections before taking a full-page capture.
          if (route === '/') {
            for (let y = 0; y < await page.evaluate(() => document.documentElement.scrollHeight); y += height * 0.7) {
              await page.evaluate(y => scrollTo(0, y), y);
              await page.waitForTimeout(60);
            }
            await page.evaluate(() => scrollTo(0, 0));
            await page.waitForTimeout(1000);
          }
          await page.screenshot({ path: path.join(output, `${width}-${route.replaceAll('/', '_') || 'home'}.png`), fullPage: true });
        }
      }
      if ([320, 844, 1440].includes(width)) await interactions(page, { origin, width, inspect, failures, output, engine });
      console.log(`${process.env.INTERACTIONS_ONLY ? 'Interactions only' : `Inspected ${routes.length} routes`} at ${width}×${height}`);
      await context.close();
    }
  } finally { await browser.close(); }
  fs.writeFileSync(path.join(output, 'results.json'), JSON.stringify({ engine, mode: process.env.INTERACTIONS_ONLY ? 'interactions' : process.env.STATES_ONLY ? 'states' : 'full', routes: routes.length, sizes, failures: [...new Set(failures)] }, null, 2));
  assert.deepEqual([...new Set(failures)], []);
  console.log(`PASS: ${process.env.INTERACTIONS_ONLY ? 'targeted interactions' : process.env.STATES_ONLY ? 'targeted state checks' : `${routes.length} populated routes × ${sizes.length} viewports, interactions and states`}. API responses are fixtures; ${engine} emulation.`);
})().catch(e => { console.error(e); process.exitCode = 1; });
