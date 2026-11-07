// Start Vite, then run with PLAYWRIGHT_MODULE and TEST_ORIGIN as needed.
// All API responses are fixtures; no live stock, accounts or emails are changed.
const assert = require('node:assert/strict');
const path = require('node:path');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const { mockApi, seed, seedData } = require('./responsive-fixtures.cjs');
const origin = process.env.TEST_ORIGIN || 'http://127.0.0.1:5173';
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));

(async () => {
  const browser = await chromium.launch({ headless: true, ...(process.env.CHROME_PATH ? { executablePath: process.env.CHROME_PATH } : {}) });
  try {
    const context = await browser.newContext({ viewport: { width: 390, height: 844 }, reducedMotion: 'reduce' });
    await context.addInitScript(seed, seedData);
    await context.addInitScript(() => {
      window.previewUrls = { created: 0, released: 0 };
      const create = URL.createObjectURL.bind(URL), revoke = URL.revokeObjectURL.bind(URL);
      URL.createObjectURL = value => { window.previewUrls.created++; return create(value); };
      URL.revokeObjectURL = value => { window.previewUrls.released++; return revoke(value); };
    });
    let staleFinished;
    let adminWrites = 0;
    let releaseSave;
    await context.route('**/guiltFreeCookie/**', async route => {
      const url = new URL(route.request().url());
      const status = url.searchParams.get('status');
      if (url.pathname.endsWith('/admin/update')) {
        adminWrites++;
        await new Promise(resolve => { releaseSave = resolve; });
        return route.fulfill({ json: route.request().postDataJSON() });
      }
      if (url.pathname.endsWith('/admin/messages') || url.pathname.endsWith('/admin/reviews')) {
        const stale = ['RESOLVED', 'APPROVED'].includes(status);
        if (stale) await delay(500);
        const result = await mockApi({ request: () => route.request(), fulfill: ({ json }) => route.fulfill({ json: json.map(item => ({ ...item, status, subject: `${status} message`, title: `${status} review` })) }) });
        if (stale) staleFinished?.();
        return result;
      }
      if (url.pathname.endsWith('/admin/orders')) {
        const stale = url.searchParams.get('q') === 'older';
        if (stale) await delay(900);
        const result = await mockApi({ request: () => route.request(), fulfill: ({ json }) => route.fulfill({ json: { ...json, items: json.items.map(item => ({ ...item, customerName: url.searchParams.get('q') || 'Initial' })) } }) });
        if (stale) staleFinished?.();
        return result;
      }
      if (url.pathname.endsWith('/admin/analytics')) {
        const days = Math.round((new Date(url.searchParams.get('to')) - new Date(url.searchParams.get('from'))) / 86400000) + 1;
        if (days === 7) await delay(500);
        const result = await mockApi({ request: () => route.request(), fulfill: ({ json }) => route.fulfill({ json: { ...json, current: { ...json.current, revenue: days === 7 ? 111 : 999 } } }) });
        if (days === 7) staleFinished?.();
        return result;
      }
      return mockApi(route);
    });
    const page = await context.newPage();
    for (const [route, oldStatus, newStatus, text] of [
      ['messages', 'RESOLVED', 'NEW', 'NEW message'],
      ['reviews', 'APPROVED', 'REJECTED', 'REJECTED review'],
    ]) {
      await page.goto(`${origin}/admindashboard/${route}`);
      await page.locator('main article').first().waitFor();
      const completed = new Promise(resolve => { staleFinished = resolve; });
      const request = page.waitForRequest(r => new URL(r.url()).searchParams.get('status') === oldStatus);
      await page.locator('main select').selectOption(oldStatus);
      await request;
      await page.locator('main select').selectOption(newStatus);
      await completed;
      // Give the delayed response time to commit; checking only network completion can miss a stale render.
      await page.waitForTimeout(100);
      await page.waitForFunction(text => document.querySelector('main article h2')?.textContent.includes(text), text);
      assert.ok((await page.locator('main article h2').textContent()).includes(text), 'newest filter must win');
    }
    await page.goto(`${origin}/admindashboard/orders`);
    await page.getByText('Initial', { exact: true }).waitFor();
    const completed = new Promise(resolve => { staleFinished = resolve; });
    const request = page.waitForRequest(r => new URL(r.url()).searchParams.get('q') === 'older');
    await page.getByLabel('Search orders', { exact: true }).fill('older');
    await request;
    await page.getByLabel('Search orders', { exact: true }).fill('latest');
    await completed;
    await page.waitForTimeout(100);
    await page.getByText('latest', { exact: true }).waitFor();
    await page.getByRole('button', { name: 'Clear filters', exact: true }).click();
    await page.waitForURL(url => !url.search);

    await page.goto(`${origin}/admindashboard/overview`);
    await page.getByText('R999.00', { exact: true }).waitFor();
    const overviewDone = new Promise(resolve => { staleFinished = resolve; });
    const overviewRequest = page.waitForRequest(r => r.url().includes('/admin/analytics'));
    await page.getByRole('button', { name: '7 days', exact: true }).click();
    await overviewRequest;
    await page.getByRole('button', { name: '90 days', exact: true }).click();
    await overviewDone;
    await page.waitForTimeout(100);
    await page.getByText('R999.00', { exact: true }).waitFor();

    await page.goto(`${origin}/admindashboard/cookies`);
    await page.getByRole('button', { name: 'Add cookie', exact: true }).click();
    const cookieForm = page.getByRole('dialog', { name: 'Add cookie', exact: true });
    for (let i = 0; i < 4; i++) {
      await cookieForm.locator('input[type=file]').setInputFiles(path.join(__dirname, '../public/img/cookies/double-chocolate-chunk.webp'));
      await cookieForm.getByRole('button', { name: 'Replace photo' }).click();
    }
    await cookieForm.getByRole('button', { name: 'Cancel', exact: true }).click();
    assert.deepEqual(await page.evaluate(() => window.previewUrls), { created: 4, released: 4 });

    await page.goto(`${origin}/admindashboard/users`);
    await page.getByRole('button', { name: 'Edit', exact: true }).first().click();
    const adminForm = page.getByRole('dialog', { name: 'Edit admin', exact: true });
    await adminForm.getByRole('button', { name: 'Save', exact: true }).click();
    await page.getByRole('dialog', { name: 'Save changes?', exact: true }).getByRole('button', { name: 'Save', exact: true }).click();
    await page.waitForFunction(() => document.querySelector('dialog[open] button[aria-busy=true]'));
    assert.equal(await adminForm.getByRole('button', { name: 'Save', exact: true }).isDisabled(), true);
    assert.equal(adminWrites, 1);
    releaseSave();
    await adminForm.waitFor({ state: 'hidden' });
    console.log('PASS: latest filters win, clear filters, photo previews released, duplicate admin saves blocked.');
    await context.close();
  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
