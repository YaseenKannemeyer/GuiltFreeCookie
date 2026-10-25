# Handoff: admin discount-codes screen (ChatGPT, frontend only)

Give the shop team a screen to create, edit and switch off discount codes. The backend is finished and tested; **don't change any Java**.

**Where to work:**
- Folder: `../guiltFreeCookie-discounts`
- Branch: `discount-codes-chatgpt`, made from `Completing-the-site`.

Don't commit to `Completing-the-site`. Leave your work uncommitted or commit it on your branch; Claude will review and merge it.

Read first:
- `PRODUCT.md`: who uses the admin, and the rules for claims.
- `docs/API.md`: how the API fits together.
- The Phase 3 discount section of `docs/API-CHANGELOG.md`: the request body and its rules.

## The API (admin token; the interceptor already adds it for `/api/v1/admin/**`)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/admin/discounts` | Every code, newest first |
| GET | `/api/v1/admin/discounts/{id}` | One code |
| POST | `/api/v1/admin/discounts` | Create. `409` if the code already exists |
| PUT | `/api/v1/admin/discounts/{id}` | Replaces every field except `usedCount`, so send the full body |
| DELETE | `/api/v1/admin/discounts/{id}` | **Switches it off** (`active: false`). It stays on record for past orders |

**Body:**
```json
{
  "code": "SPRING15",
  "type": "PERCENT",
  "value": 15,
  "minSubtotal": 200,
  "startsAt": null,
  "endsAt": "2026-12-01T00:00:00",
  "maxUses": null,
  "onePerEmail": true,
  "restrictedToEmail": null,
  "description": "Spring box promotion",
  "active": true
}
```

**Response:** the same fields, plus `discountId`, `usedCount` and `createdAt`.

**Rules the server enforces.** Show its `message` as is when it says no.
- `code`: 3–30 letters, numbers, `-` or `_`. It's stored in upper case.
- `type`:
  - `PERCENT`: `value` from 1 to 100.
  - `FIXED`: `value` is a rand amount above 0.
  - `FREE_SHIPPING`: no `value`. It makes courier and PUDO delivery free.
- `minSubtotal`: optional, 0 or more, after other discounts.
- `startsAt` and `endsAt`: optional local date-times, and the end must be after the start. Empty means "always".
- `maxUses`: optional, at least 1. `1` makes a single-use code. A use is counted when an order is **paid**, not when a code is applied.
- `onePerEmail`: each customer email can use it once.
- `restrictedToEmail`: only that email can use it.

## Build

1. **Route and nav:**
   - Add `/admindashboard/discounts` (name `AdminDiscounts`) in `frontend/src/router/routes.js`. Add it to the existing admin array next to "email-flows", with `meta.requiresAdmin`.
   - Add **Discount codes** to `frontend/src/layout/AdminShell.vue` after "Delivery", using the `TicketIcon` from `@heroicons/vue/24/outline`.
2. **API calls:** a new `frontend/src/api/discountApi.js`, using the `api` helper from `storeApi.js`. Follow `frontend/src/api/flowApi.js` for the pattern.
3. **`frontend/src/views/AdminDiscountsView.vue`:**
   - **Two lists:**
     - **Shop codes:** codes with no `restrictedToEmail`.
     - **Personal codes:** with a `restrictedToEmail`. These are mostly the `WELCOME-…`, `CART-…` and `MISSYOU-…` codes the email flows issue. Show this list collapsed by default, with a count, so it doesn't bury the shop codes.
   - **Each row shows:**
     - the code, in a monospace or tabular style;
     - what it gives, e.g. "15% off", "R50 off" or "Free delivery", plus "on orders from R200" when `minSubtotal` is set;
     - when it runs ("Until 1 Dec 2026", "Always", "Starts 1 Nov");
     - uses ("3 used", or "3 of 50 used");
     - a status badge: **Active**, **Scheduled** (`startsAt` is in the future), **Expired** (`endsAt` has passed), **Used up** (`usedCount` ≥ `maxUses`) or **Off** (`active` is false). Compute it on the client.
   - **Create and edit:**
     - Use an inline form or a drawer, in the field style of `AdminShippingView.vue`.
     - Choose the type with three radio tiles. Hide `value` for free delivery, and label it "%" or "R" for the others.
     - Use date inputs, not date-time ones, for start and end. Send `T00:00:00` for the start, and the day **after** the chosen end date at `T00:00:00`, so that "until 1 Dec" includes 1 Dec.
     - Put the extra rules (minimum order, max uses, one per customer, only this email) under "More rules", collapsed unless any is set.
     - Upper-case the code as the admin types.
   - **Switch off:** a "Switch off" button that confirms inline (not `window.confirm`), explaining that past orders keep the code. Bring a code back by editing it and ticking "Active".
   - **States:**
     - Empty: "No discount codes yet", with a "Create a code" button.
     - Loading: a skeleton, like the other admin pages.
     - Errors: the server's `message` in the alert style used by `AdminShippingView.vue`.

## Match the existing admin

- **Look:** follow `AdminShippingView.vue` and `AdminEmailFlowsView.vue` for layout, spacing and headings. Use Luckiest Guy (`font-display`) only for the page title; section headings are `text-[2rem] font-extrabold`.
- **Components:** `BaseButton`, `BaseInput` and the `.field` class. Badges use the pill style from `constants/orderStatus.js`. Tailwind's `1rem` is 10px here.
- **Phones:** it must work at 390px wide with no sideways scrolling. Grid children holding inputs need `min-w-0`. Rows become stacked cards on mobile, like `AdminOrdersView.vue`.
- **Wording:** keep it short and plain.

## Done means

- `cd frontend && npm run build` passes.
- Checked in a browser against the real backend (`./mvnw spring-boot:run` or the test runner):
  - create a percent code, a fixed code and a free-delivery code;
  - edit one;
  - switch one off;
  - see an expired and a used-up code labelled correctly;
  - at 390px and at 1280px.
- Apply a code you created in the storefront cart to prove it works end to end.
- Add a short dated entry to the top of `docs/API-CHANGELOG.md` (frontend only, no API change).
