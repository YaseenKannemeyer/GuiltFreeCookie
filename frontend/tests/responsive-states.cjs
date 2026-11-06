const assert = require('node:assert/strict');
const { mockApi, seed, seedData } = require('./responsive-fixtures.cjs');
module.exports = async function states(browser, { origin, inspect, failures }) {
  for (const width of [320, 768, 1440]) {
    const context = await browser.newContext({ viewport: { width, height: 740 }, reducedMotion: 'reduce' });
    await context.route('**/guiltFreeCookie/**', mockApi);
    const page = await context.newPage();
    page.on('pageerror', e => failures.push(`${width} states: ${e.message}`));
    await page.goto(origin + '/admin');
    await page.getByRole('heading', { name: 'Admin log in' }).waitFor();
    await inspect(page, `${width} guest admin login`, failures);
    await context.addInitScript(seed, seedData);
    for (const mode of ['loading', 'empty', 'error']) {
      let release;
      const gate = new Promise(resolve => { release = resolve; });
      const handler = async route => {
        if (mode === 'loading') await gate;
        await route.fulfill(mode === 'error' ? { status: 503, json: { message: 'Temporarily unavailable. Please try again.' } } : { json: [] });
      };
      await context.route('**/admin/getAll', handler);
      await page.goto(origin + '/admindashboard/users');
      await page.getByRole('heading', { name: 'Users', exact: true }).waitFor();
      if (mode !== 'loading') await page.getByText(mode === 'empty' ? 'No admins yet' : "Couldn't load admins", { exact: true }).waitFor();
      if (mode === 'loading') await page.getByLabel('Loading admins').waitFor();
      await inspect(page, `${width} admin ${mode}`, failures);
      release();
      await context.unroute('**/admin/getAll', handler);
    }
    const fail = route => route.fulfill({ status: 503, json: { message: 'Temporarily unavailable. Please try again.' } });
    await context.route('**/api/v1/cookies?*', fail);
    await page.goto(origin + '/products');
    await page.getByRole('button', { name: 'Try again', exact: true }).waitFor();
    await inspect(page, `${width} catalog error`, failures);
    await context.unroute('**/api/v1/cookies?*', fail);
    await page.getByRole('button', { name: 'Try again', exact: true }).click();
    await page.getByRole('heading', { name: 'Double Chocolate Chunk', exact: true }).waitFor();
    assert.equal(await page.getByRole('button', { name: 'Try again', exact: true }).count(), 0);
    await context.route('**/api/v1/carts/recover/expired', fail);
    await page.goto(origin + '/cart/recover/expired');
    await page.getByRole('heading', { name: 'That cart has moved on' }).waitFor();
    await inspect(page, `${width} expired cart link`, failures);
    await context.close();
    console.log(`PASS: guest login, admin loading/empty/error, catalog retry, expired cart at ${width}px`);
  }
};
