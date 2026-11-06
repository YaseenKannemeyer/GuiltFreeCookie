const assert = require('node:assert/strict');

module.exports = async function interactions(page, { origin, width, inspect, failures, output }) {
  const visit = async route => { await page.goto(origin + route); await page.locator('h1').first().waitFor(); };
  await visit('/admindashboard/users');
  const opener = page.getByRole('button', { name: 'Add admin', exact: true });
  await opener.click();
  const form = page.getByRole('dialog', { name: 'Add admin', exact: true });
  await form.waitFor();
  await inspect(page, `${width} add admin dialog`, failures);
  for (let i = 0; i < 10; i++) {
    await page.keyboard.press('Tab');
    assert.ok(await form.evaluate(el => el.contains(document.activeElement)), 'Modal must contain keyboard focus');
  }
  await form.getByLabel('First name').fill('Alexandra');
  await form.getByLabel('Last name').fill('Baker');
  await form.getByLabel(/^Email/).fill('alexandra.baker@example.com');
  await form.getByLabel(/^Password/).fill('fixture-only-password');
  await form.getByRole('button', { name: 'Save', exact: true }).click();
  const confirm = page.getByRole('dialog', { name: 'Add admin?', exact: true });
  await confirm.waitFor();
  await inspect(page, `${width} nested confirmation`, failures);
  await page.keyboard.press('Escape');
  await confirm.waitFor({ state: 'hidden' });
  assert.equal(await page.evaluate(() => document.body.style.overflow), 'hidden');
  assert.ok(await form.evaluate(el => el.contains(document.activeElement)), 'Restore focus to parent modal');
  await form.getByRole('button', { name: 'Save', exact: true }).click();
  await confirm.getByRole('button', { name: 'Save', exact: true }).click();
  const toast = form.getByRole('status').filter({ hasText: 'An admin with that email already exists.' });
  await toast.waitFor();
  await toast.getByRole('button', { name: 'Dismiss' }).click();
  await form.getByRole('button', { name: 'Close', exact: true }).click();
  await form.waitFor({ state: 'hidden' });
  assert.equal(await page.evaluate(() => document.body.style.overflow), '');
  assert.ok(await opener.evaluate(el => el === document.activeElement), 'Restore focus to opener');

  await page.getByRole('button', { name: 'Delete', exact: true }).last().click();
  const deletion = page.getByRole('dialog', { name: 'Delete admin?' });
  const hold = deletion.getByRole('button', { name: 'Hold to delete' });
  await hold.waitFor();
  await hold.focus();
  await page.keyboard.down('Space');
  await page.waitForTimeout(150);
  await page.keyboard.up('Space');
  assert.equal(await hold.getAttribute('aria-busy'), 'false', 'Early release cancels deletion');
  if (width < 1100) {
    await hold.scrollIntoViewIfNeeded();
    const b = await hold.boundingBox();
    const client = await page.context().newCDPSession(page);
    await client.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x: b.x + b.width / 2, y: b.y + b.height / 2 }] });
    await client.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ x: b.x + b.width / 2, y: b.y + b.height / 2 - 80 }] });
    await client.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
    await page.waitForTimeout(1700);
    assert.equal(await hold.getAttribute('aria-busy'), 'false', 'Swipe across hold control cancels deletion');
    assert.equal(await deletion.getByRole('button', { name: 'Undo' }).count(), 0);
    await client.detach();
  }
  await hold.focus();
  await page.keyboard.down('Space');
  await deletion.getByRole('button', { name: 'Undo' }).waitFor();
  await page.keyboard.up('Space');
  await deletion.getByRole('button', { name: 'Undo' }).click();
  await hold.waitFor();
  await page.keyboard.press('Escape');

  await visit('/admindashboard/cookies');
  await page.getByRole('button', { name: 'Add cookie', exact: true }).click();
  const cookieForm = page.getByRole('dialog', { name: 'Add cookie', exact: true });
  await cookieForm.waitFor();
  await inspect(page, `${width} cookie form`, failures);
  await cookieForm.locator('input[type=file]').setInputFiles(require('node:path').join(__dirname, '../public/img/cookies/double-chocolate-chunk.webp'));
  await cookieForm.getByRole('button', { name: 'Replace photo' }).waitFor();
  await inspect(page, `${width} selected photo`, failures);
  await page.waitForFunction(() => [...document.querySelectorAll('dialog[open] .modal-panel')].every(el => getComputedStyle(el).opacity === '1'));
  if (output) await page.screenshot({ path: `${output}/${width}-cookie-dialog.png` });
  await cookieForm.getByLabel('Allergens').fill('Almonds');
  await cookieForm.getByRole('button', { name: 'Cancel', exact: true }).click();
  assert.equal(await page.evaluate(() => document.body.style.overflow), '');

  await visit('/checkout/delivery');
  const search = page.getByRole('combobox', { name: 'Search your address' });
  await search.fill('12 Main');
  const choice = page.getByRole('option', { name: '12 Main Road Rondebosch, Cape Town' });
  await choice.waitFor();
  if (width < 1100) await choice.tap(); else await choice.click();
  await page.waitForFunction(() => [...document.querySelectorAll('input')].some(el => el.value === 'Main Road'));
  await inspect(page, `${width} selected address`, failures);

  await visit('/admindashboard/overview');
  const chart = page.getByRole('img', { name: 'Revenue by day. Use the arrow keys to read each day.' });
  await chart.focus();
  await page.keyboard.press('End');
  await page.getByRole('status').filter({ hasText: 'R1000.00' }).waitFor();
  await page.keyboard.press('Home');
  await page.getByRole('status').filter({ hasText: 'R500.00' }).waitFor();
  if (width < 1100) await chart.tap();
  await page.getByText('Show as a table', { exact: true }).click();
  await inspect(page, `${width} chart details`, failures);

  // Double text size at an ordinary desktop width exercises reflow independently of viewport tests.
  if (width === 1440) {
    await visit('/admindashboard/users');
    await page.evaluate(() => document.documentElement.style.fontSize = '125%');
    await inspect(page, '1440 at 200% root text size', failures);
    await page.evaluate(() => document.documentElement.style.fontSize = '');
  }
  if (output) await page.screenshot({ path: `${output}/${width}-checkout-address.png`, fullPage: true });
  console.log(`PASS: dialogs, focus, notifications, hold cancellation/undo, long form, address selection at ${width}px`);
};
