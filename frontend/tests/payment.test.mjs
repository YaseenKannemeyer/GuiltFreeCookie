import test from 'node:test';
import assert from 'node:assert/strict';
import { submitPaymentForm } from '../src/api/payment.js';

function fakeBrowser() {
  const posted = [];
  let destination;
  globalThis.window = { location: { assign: value => { destination = value; } } };
  globalThis.document = {
    body: { append: form => posted.push(form) },
    createElement: tag => tag === 'form' ? { children: [], append(input) { this.children.push(input); }, submit() { this.submitted = true; } } : {},
  };
  return { posted, destination: () => destination };
}

test('hosted checkout posts signed fields to the two exact PayFast endpoints', () => {
  const browser = fakeBrowser();
  for (const host of ['www.payfast.co.za', 'sandbox.payfast.co.za']) {
    submitPaymentForm({ gateway: 'PAYFAST', actionUrl: `https://${host}/eng/process`, fields: { amount: '50.00', signature: 'signed' } }, { demoMode: false });
  }
  assert.equal(browser.posted.length, 2);
  assert.ok(browser.posted.every(form => form.method === 'POST' && form.submitted));
  assert.deepEqual(browser.posted[0].children.map(i => [i.name, i.value]), [['amount', '50.00'], ['signature', 'signed']]);
});

test('untrusted payment destinations cannot receive checkout data', () => {
  const browser = fakeBrowser();
  for (const actionUrl of ['https://www.payfast.co.za.evil.test/eng/process', 'http://www.payfast.co.za/eng/process', 'javascript:alert(1)', 'https://www.payfast.co.za/eng/process?next=evil']) {
    assert.throws(() => submitPaymentForm({ actionUrl, fields: { amount: '50' } }));
  }
  assert.equal(browser.posted.length, 0);
});

test('demo checkout navigates locally and never posts to a payment provider', () => {
  const browser = fakeBrowser();
  submitPaymentForm({ gateway: 'DEMO', actionUrl: '/demo-payment', fields: { order: 'GFC-DEMO', token: 'private&token' } });
  const target = new URL(browser.destination(), 'http://127.0.0.1:5173');
  assert.equal(target.pathname, '/demo-payment');
  assert.equal(target.searchParams.get('token'), 'private&token');
  assert.equal(browser.posted.length, 0);
});

test('demo mode refuses a live PayFast handoff', () => {
  const browser = fakeBrowser();
  assert.throws(() => submitPaymentForm({ gateway: 'PAYFAST', actionUrl: 'https://www.payfast.co.za/eng/process', fields: { amount: '50' } }), /Live payments/);
  assert.equal(browser.posted.length, 0);
});
