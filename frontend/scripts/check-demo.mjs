// Read-only check. Never creates an order, sends mail or changes inventory.
import assert from "node:assert/strict";
const base = (process.env.PRERENDER_API_URL || "http://127.0.0.1:8080/guiltFreeCookie").replace(/\/$/, "");
async function read(path) {
  const response = await fetch(`${base}${path}`, { signal: AbortSignal.timeout(10000) });
  assert.equal(response.status, 200, `${path} must respond successfully`);
  return response.json();
}
assert.equal((await read("/actuator/health")).status, "UP");
assert.equal((await read("/actuator/health/readiness")).status, "UP");
assert.equal((await read("/actuator/health/dependencies")).status, "UP");
const list = await read("/api/v1/cookies?size=48");
assert.ok(list.items.length >= 12, "The demo needs all 12 sample products");
for (const item of list.items) {
  const cookie = await read(`/api/v1/cookies/${encodeURIComponent(item.slug || item.cookieId)}`);
  assert.ok(cookie.ingredients && cookie.allergies, `${cookie.name}: ingredients and allergens`);
  assert.ok(cookie.variants.length >= 3, `${cookie.name}: single, 6-pack and 12-pack`);
  assert.ok(cookie.variants.every(v => Number(v.price) > 0), `${cookie.name}: positive prices`);
  const image = await fetch(`${base}/uploads/${cookie.image}`, { signal: AbortSignal.timeout(10000) });
  assert.equal(image.status, 200, `${cookie.name}: image available`);
}
console.log(`Demo health, ${list.items.length} products, pack prices and images passed.`);
