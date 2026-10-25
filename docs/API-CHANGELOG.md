# API Changelog

Base URL: `http://localhost:8080/guiltFreeCookie`. Swagger UI: `/swagger-ui.html`, OpenAPI JSON: `/v3/api-docs` (disabled in prod unless `API_DOCS_ENABLED=true`).

Auth: admin endpoints need `Authorization: Bearer <token>` from `POST /admin/login` (`{admin, token}`). Anything not listed as public in `SecurityConfig` requires an admin.

## 2026-10-25 — Phase 7: Automated email flows

Seven email sequences that send themselves, built on Spring Mail. A job checks every 5 minutes (`app.flows.run-ms`) and sends whatever is due.

| Flow | Starts when | Emails |
|---|---|---|
| Welcome | Someone subscribes (footer, register or checkout opt-in). Once per address, ever | Now (personal single-use code, default 10%, lasts 5 days), +24h favourites, +48h how we bake, +72h boxes and real reviews, +96h "code ends tomorrow" (skipped if used or expired) |
| Browsed, didn't add | `POST /api/v1/events/product-view` from a signed-in shopper or a cart with an email. At most once a week | +1h the cookie, +24h similar cookies |
| Left in the cart | A cart with an email and items sits untouched for 1 hour (checked every 15 min) | Now, +23h delivery facts, +47h code (default 10%, 3 days) |
| After delivery | An admin marks the order `DELIVERED` | +3 days storage tips, +7 days review request (review-by-link page) |
| Refill | Same trigger | +21 days (configurable) "reorder" with a one-tap link |
| Win back | Daily at 09:00: a subscriber's last paid order was 60 days ago (configurable) | Now new cookies, +3 days favourites, +7 days code (default 15%, 7 days) |
| Quiet subscribers | Daily: a subscriber hasn't opened or clicked for 90 days | Now "still want us?", +48h favourites, +72h last call. No click within 48h after that pauses them |

**Rules every flow follows:**
- **Consent (POPIA s69):** Welcome, Win back and Quiet need an active newsletter subscription. Cart and browse reminders need a subscription or a past paid order. A guest who only typed their email at checkout has not agreed to marketing, so they don't get reminders. After-delivery and Refill use the existing-customer rule. An unsubscribe or a pause stops all of them.
- **Ending a flow:** a purchase ends Welcome, Browse, Cart, Win back and Refill. Any marketing unsubscribe ends everything. Order and account emails are not affected.
- **Pacing:**
  - At most one flow email per address every 16 hours.
  - Nothing goes out between 20:00 and 08:00 shop time (`Africa/Johannesburg`).
  - The first welcome email is the only exception to both.
- **Codes:** the codes are ordinary discount codes, single-use and restricted to the recipient's email, so they show up in the admin discount list.
- **Every email has:**
  - An unsubscribe link to the storefront page.
  - `List-Unsubscribe` and `List-Unsubscribe-Post: List-Unsubscribe=One-Click` headers (RFC 8058, required by Gmail and Yahoo for bulk senders).
  - A plain-text part, an open pixel and tracked links.
- **Reviews:** only real approved reviews appear. With none, the block is left out.

### Public endpoints

| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/events/product-view` | `{cookieId, cartId?}`. Optional customer token. Always `202`; anonymous views are ignored |
| GET | `/api/v1/carts/recover/{token}` | The saved guest cart from a reminder email. Store its `cartId`. `404` once it's ordered, cleared or belongs to an account |
| POST | `/api/v1/orders/{orderNumber}/reorder?token=` | A **new** guest cart holding whatever from the order is still in stock (`201`). `409` if nothing is. Merge it after login |
| POST | `/api/v1/reviews/by-link` | `{orderNumber, token, cookieId, rating, title, body, authorName}`. The order must be `DELIVERED` and contain the cookie. Rate-limited to 10 an hour |
| GET | `/api/v1/email/o/{sendId}.gif` | Open pixel |
| GET | `/api/v1/email/c/{sendId}?u=` | Tracked link. Redirects only to the storefront (`FRONTEND_BASE_URL`); anything else goes to the home page |
| POST | `/api/v1/email/unsubscribe` | `{email, token}` → `204`. `400` for a bad token |
| POST | `/api/v1/email/unsubscribe/one-click?e=&t=` | For mail apps (RFC 8058) |
| POST | `/api/v1/email/resubscribe`, `/api/v1/email/stay` | `{email, token}`: undo an unsubscribe, or "keep me subscribed" |

The `email` and `token` (`t`) values come from the link in the email. The token is an HMAC of the address (`EMAIL_LINK_SECRET`, which defaults to `JWT_SECRET`).

### Admin

- `GET | PUT /api/v1/admin/email-flows`: an on/off switch per flow, discount percentages (`0` sends no code), welcome code days, refill, win-back and quiet-subscriber days. Missing fields keep their value.
- `GET /api/v1/admin/email-flows/stats?days=30`: `[{flow, sent, opened, clicked}]` for every flow. Opens are approximate because Apple Mail and others preload images.
- Email previews now list every step (`flow-welcome-1` … `flow-sunset-3`).

### Also changed

- **Newsletter subscribers:** gained `status` (`ACTIVE`, `PAUSED` or `UNSUBSCRIBED`; null on old rows means active), `source`, `lastEngagedAt` and `statusChangedAt`. Subscribing again after unsubscribing counts as a fresh opt-in.
- **Order events:** marking an order `DELIVERED` now publishes `OrderEvents.Delivered`.
- **Unsubscribe links:** marketing email still needs an HTTPS unsubscribe link, but `http://localhost` is now allowed so the flows work in local development.
- **New tables:** `flow_enrollments`, `email_sends`, `email_suppressions` and `email_flow_settings` (created by `ddl-auto=update`). Unsubscribes from people who never subscribed go in `email_suppressions`.
- **Frontend:**
  - New pages: `/email/unsubscribe`, `/email/subscribed`, `/cart/recover/:token`, `/reorder/:orderNumber`, `/review/:orderNumber` and admin **Email flows**.
  - Product pages send the view signal.
  - A checkout opt-in is now recorded when the shopper leaves the "You" step, not only at payment.
  - The privacy policy explains the automated emails and the open and click tracking.

### Config (all optional)

| Setting | Effect |
|---|---|
| `FLOWS_ENABLED=false` | Seed the settings with every flow off |
| `EMAIL_LINK_SECRET` | Rotate unsubscribe links separately from JWTs |
| `app.flows.*` | Defaults, intervals and `daily-cron`. `-` switches off the daily check |

## 2026-10-25 — Phase 5: Customer accounts

All routes below require a **customer** bearer token. Anonymous requests return 401; admin tokens return 403. Existing profile/password, guest tracking and backend checkout contracts are unchanged.

### Order history

- `GET /api/v1/me/orders?page=0&size=10` → `{items, page, size, totalItems, totalPages}`. Page is zero-based; size is 1–100. Newest first, with a stable order-ID tie-breaker. Only the signed-in email's `accountOwned=true` orders appear. Expired pending reservations and never-paid cancelled attempts are excluded; previously paid cancelled/refunded orders remain.
- Items: `{orderNumber, createdAt, status, shippingMethod, itemCount, total, image, token}`. `itemCount` sums pack quantities. `image` is the current cookie image for the first line (stable item-ID order), or null if unavailable. Lines and images are fetched in batches. Link to `/order/{orderNumber}?token={token}` using the existing tracking page.
- `GET /api/v1/me/orders/{orderNumber}` → the existing full `OrderView` fields **at the top level**, plus `token`. An unowned or missing order returns the same 404.

### Saved addresses

| Method | Path | Response |
|---|---|---|
| GET | `/api/v1/me/addresses` | Array, newest first |
| POST | `/api/v1/me/addresses` | 201, saved address |
| PUT | `/api/v1/me/addresses/{id}` | Updated saved address |
| DELETE | `/api/v1/me/addresses/{id}` | 204 |
| PUT | `/api/v1/me/addresses/{id}/default` | Updated saved address |

Create/update body:
```json
{"label":"Home","recipientName":"Thandi Mokoena","phone":"0821234567","isDefault":false,
 "address":{"streetNumber":"12","streetName":"Bakery Lane","complex":null,"suburb":"Gardens",
 "city":"Cape Town","province":"WESTERN_CAPE","postalCode":"0002"}}
```
Response: `{id, label, recipientName, phone, isDefault, createdAt, address}`. The postal code remains a **four-digit String**. Label is required, max 30; recipient name required, max 200; phone required, max 30 and a valid SA mobile. Address validation matches checkout: street number max 20, other text parts max 100, province required, complex optional. Invalid data returns 400.

Maximum 10 per customer (409 on an eleventh). The first becomes default. Choosing another default clears the previous one; deleting the default promotes the newest remaining address. Editing the current default with `isDefault:false` keeps it default until a replacement is chosen. Ownership failures return 404. Writes lock the customer row to serialize cap/default changes, including simultaneous first-address creation. Legacy delivery records without a customer remain available through the existing admin routes, excluded from the account book; nullable legacy default flags read as false.

### Claim earlier guest orders

- `GET /api/v1/me/orders/claimable` → `{count}` only, never order details. Eligible orders match the account email, have `accountOwned=false` and a non-null `paidAt` (including later refunded/cancelled paid orders).
- `POST /api/v1/me/orders/claim` → 204. If eligible orders exist, sends the branded `claim-orders` email to the **account's own inbox**. No caller-supplied recipient. Rate limited to 3 requests per hour per IP (429 with `Retry-After`). Available in admin email previews.
- Email link: `/account/claim?token=…`. Uses a separate HMAC-SHA256 secret and expires after 24 hours. It's signed with `ACCOUNT_CLAIM_SECRET`, which defaults to `JWT_SECRET` (the same pattern as order and unsubscribe links). Set it to rotate claim links separately; rotating it invalidates outstanding links.
- `POST /api/v1/me/orders/claim/confirm` with `{"token":"…"}` → `{claimed}`. Verifies signature, expiry and equality with the signed-in email before reading/updating eligible orders. Invalid, expired and other-email links return 400. Claims are transactional and serialized; repeating a valid link returns 0 after all eligible orders have been claimed. Links remain valid until expiry and can claim newly paid eligible orders during that period.

### Frontend

Account now has Orders and Addresses above the existing profile/password forms, mobile anchors and desktop section navigation, paged order rows, inbox-link prompt, inline address editing and inline delete confirmation. `/account/claim` preserves its token through sign-in and shows success/error feedback.

Signed-in courier checkout offers saved-address choice tiles, fills the default when no address draft exists, and copies the selected address into the unchanged checkout payload. An optional, initially unchecked save runs immediately before placing the order; a failure (including 401) shows a notice and **still submits checkout**, with a four-second timeout. The selection and save preference survive checkout step navigation.

### Database

`ddl-auto=update` adds these nullable columns to `delivery_details`; existing rows require no backfill. For manually managed MySQL schemas, add only columns not already present:
```sql
ALTER TABLE delivery_details
  ADD COLUMN customer_email VARCHAR(254) NULL,
  ADD COLUMN label VARCHAR(30) NULL,
  ADD COLUMN recipient_name VARCHAR(200) NULL,
  ADD COLUMN phone VARCHAR(30) NULL,
  ADD COLUMN is_default BIT NULL,
  ADD COLUMN created_at DATETIME(6) NULL;
```
Keep the Phase 4 `postal_code VARCHAR(10)` migration. No order-table migration is needed.

### Validation

`./mvnw test`: 378 tests, zero failures/errors, one existing skip. `cd frontend && npm run build`: passes (product prerendering still needs `PRERENDER_API_URL`). Browser regression passes at 390px and 1280px, including optional-save 401 continuation and signed-out claim redirect preservation.

Backend unit, repository, security and real H2/HTTP integration tests cover ownership, history filtering/pagination, claim signatures/expiry/identity/idempotence, rate limiting, address limits, default switching and deletion promotion. The browser regression is `frontend/tests/phase5.browser.cjs` (Vite on port 5175; `PLAYWRIGHT_MODULE` and `CHROME_PATH` can select the local installation). It uses mocked HTTP fixtures; it does not verify a real PayFast payment or Mailpit delivery.


## 2026-10-24 — Phase 4: Checkout, orders, shipping & PayFast

### ⚠️ Action required before deploying
1. **Database:** `Address.postalCode` is now text, so codes like `0002` keep their leading zero. Hibernate won't change an existing column's type, so run this once on MySQL:
   `ALTER TABLE delivery_details MODIFY postal_code VARCHAR(10);`
2. **PayFast:** create a free sandbox account at sandbox.payfast.co.za (Settings → Integration). Set a passphrase there, then set `PAYFAST_MERCHANT_ID`, `PAYFAST_MERCHANT_KEY` and `PAYFAST_PASSPHRASE`. The shared test merchant `10000100` **rejects signatures**, so use your own.
3. **Public URLs:** PayFast must be able to reach `PAYFAST_NOTIFY_URL` (it defaults to `PUBLIC_BASE_URL` + `/api/v1/payments/payfast/notify`). For local testing, run `cloudflared tunnel --url http://localhost:8080` and set `PUBLIC_BASE_URL` to the tunnel URL + `/guiltFreeCookie`. `FRONTEND_BASE_URL` is where customers return after paying.

### Delivery options

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/carts/{cartId}/shipping-options` | `[{method, label, cost, eta, freeShipping, requiresAddress, details}]`. Free delivery and FREE_SHIPPING codes are already applied |
| GET | `/api/v1/carts/{cartId}?shippingMethod=COURIER` | The cart with `shippingMethod`, `shipping` and a `total` that includes delivery. **Show this total on the checkout page**: it's exactly what checkout charges |

Methods: `COURIER` (needs an address), `PUDO` (needs a locker name), `PICKUP` (`details` is the pickup address and instructions). Defaults: courier R80, PUDO R60, free over R500 for both, pickup free. Admins change these at `/api/v1/admin/shipping-settings`.

### Checkout (public; a customer token is optional)

**`POST /api/v1/checkout`**:
```json
{ "cartId": "…", "email": "thandi@example.co.za", "firstName": "Thandi", "lastName": "Mokoena",
  "phone": "082 123 4567", "shippingMethod": "COURIER",
  "address": { "streetNumber": "12", "streetName": "Bakery Lane", "complex": "Unit 4, Oak Court",
               "suburb": "Strandfontein", "city": "Cape Town", "province": "WESTERN_CAPE", "postalCode": "7798" },
  "pudoLocker": null, "note": "Leave at the gate", "newsletterOptIn": true }
```
- `phone` is required for COURIER and PUDO. `address` is only for COURIER; `pudoLocker` is only for PUDO.
- `province` is one of `EASTERN_CAPE`, `FREE_STATE`, `GAUTENG`, `KWAZULU_NATAL`, `LIMPOPO`, `MPUMALANGA`, `NORTH_WEST`, `NORTHERN_CAPE`, `WESTERN_CAPE`.
- When signed in, `email` is ignored and the account email is used.

**`201`** returns `{orderNumber, accessToken, status, total, payment: {gateway, actionUrl, fields}}`
- **Store `orderNumber` and `accessToken`** (they make up the order link), then **POST a form** to `payment.actionUrl` with one hidden input per entry in `payment.fields`, in the given order, and submit it. That sends the customer to PayFast.
- `payment` is `null` and `status` is `PAID` when nothing is owed (100% discount). Go straight to the order page.
- Stock is held for **30 minutes**. Unpaid orders are then cancelled and the cart reopens.

Errors (show `message`):
- `400`: missing or invalid details (email, name, SA mobile number, address, 4-digit postal code, locker), or an empty cart.
- `404`: cart not found, or someone else's account cart.
- `409`: the cart was already checked out; an item changed or sold out ("Please review your cart"); the discount no longer applies; or "You've already used code X".
- `429`: too many checkouts (20 per hour per IP).

### After PayFast

PayFast sends the customer back to:
- **Paid or "back to shop":** `FRONTEND_BASE_URL/order/{orderNumber}?token={accessToken}`. The status can stay `PENDING_PAYMENT` for a few seconds until PayFast's notification arrives, so poll every few seconds while it's pending.
- **Cancelled:** `FRONTEND_BASE_URL/checkout/cancelled?order={orderNumber}&token={accessToken}`. Call the cancel endpoint below, then send them back to their cart, which is active again.

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/orders/{orderNumber}?token=` | `OrderView`: the thank-you and tracking page. `404` for a wrong token |
| POST | `/api/v1/orders/{orderNumber}/payment?token=` | A fresh `PaymentRedirect` ("Pay now" again) while `canPay` is true; `409` after that |
| POST | `/api/v1/orders/{orderNumber}/cancel?token=` | Cancel an unpaid order: the stock is released and the cart reopens. `409` if it isn't unpaid |

`OrderView` = `{orderNumber, status, createdAt, paidAt, email, firstName, lastName, phone, shippingMethod, address, pudoLocker, pickupDetails, deliveryEta, items: [{cookieId, name, variantLabel, unitsPerPack, unitPrice, quantity, lineTotal}], subtotal, discountCode, discountTotal, shippingTotal, vatIncluded, total, courierName, trackingNumber, trackingUrl, timeline: [{status, note, at}], payBy, canPay, canCancel}`

Statuses: `PENDING_PAYMENT` → `PAID` → `PROCESSING` → `READY_FOR_PICKUP` (pickup orders) or `SHIPPED` → `DELIVERED`, with `PAYMENT_FAILED`, `CANCELLED` and `REFUNDED` on the side.

Emails are sent automatically: order confirmation (on payment), shipped (with tracking) and ready for pickup. Each one links to the order page with its token.

### Admin orders (admin token)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/admin/orders?q=&status=&from=&to=&page=&size=` | `PageResponse<OrderSummary>` (`{orderNumber, createdAt, customerName, email, shippingMethod, itemCount, total, status}`), newest first. `q` matches order number, email or last name. Dates are `yyyy-MM-dd`, inclusive |
| GET | `/api/v1/admin/orders/{orderNumber}` | `{order: OrderView, customerNote, events: [{status, note, actor, createdAt}], payments: [...]}` |
| PUT | `/api/v1/admin/orders/{orderNumber}/status` | `{status, note?}`. Allowed moves: PAID→PROCESSING, PROCESSING→READY_FOR_PICKUP (pickup only; emails the customer), READY_FOR_PICKUP or SHIPPED→DELIVERED, PENDING_PAYMENT→CANCELLED (frees stock). Anything else is a `409` |
| PUT | `/api/v1/admin/orders/{orderNumber}/tracking` | `{courierName, trackingNumber, trackingUrl?}` (`https://` only). Marks a courier or PUDO order SHIPPED and emails the customer; call again to correct the details |
| POST | `/api/v1/admin/orders/{orderNumber}/refund` | `{note?}`. Records the refund; restocks if the order hadn't shipped. **The money itself is paid back in the PayFast dashboard** |
| GET/PUT | `/api/v1/admin/shipping-settings` | `{courierEnabled, courierRate, courierEta, pudoEnabled, pudoRate, pudoEta, pickupEnabled, pickupAddress, pickupInstructions, pickupEta, freeShippingThreshold}`. An empty threshold switches free delivery off |

### Also changed
- Cart lines gained `allergens` (the cookie's allergen text), so the cart and checkout can show them before purchase.
- `CartView` gained `shippingMethod`; `shipping` is filled in when you ask for a method.
- Reviews now get a real "Verified buyer" badge (a paid order containing that cookie).
- `/api/v1/site/info` `stats.customersServed` and `stats.cookiesSold` are now live (paid, unrefunded orders; cookies are counted individually).
- `Address` (used by delivery details) gained `complex`, `suburb` and `province`; `postalCode` is now a string.

## 2026-10-24 — Phase 6: Engagement, branded email, and frontend integration

Implemented on `phase-6-chatgpt`, based on Phase 2 commit `bd3a6c3`. Cart, order, payment and shipping code remains owned by the Phase 3–5 branch.

### Contact and site information

| Method | Path | Access / result |
|---|---|---|
| POST | `/api/v1/contact` | Public, **204**. `{name, email, phone?, subject, message, website?}`. A non-empty `website` silently drops the message. Limit: **5 per hour per client IP**; 429 includes `Retry-After`. |
| GET | `/api/v1/site/info` | Public. `{supportEmail, phone, whatsapp, hours, responseTime, address, social:{instagram,facebook,tiktok}, stats:{averageRating,reviewCount,customersServed,cookiesSold}}`. |
| GET | `/api/v1/admin/messages?status=NEW\|RESOLVED` | Admin. Newest first; omit status for all. |
| PUT | `/api/v1/admin/messages/{id}/resolve` | Admin. Idempotent; records `resolvedAt` and authenticated admin ID in `resolvedBy`. |

Contact limits: name 100, email 254, phone 30 (optional SA mobile), subject 150, message 5000 characters. Inputs use `Helper`; invalid inputs return the standard 400 error. Saving queues a shop notification and customer auto-reply through `EmailService`. SMTP failure is logged and does not remove the saved contact message.

`ContactMessage` = `{id,name,email,phone,subject,message,createdAt,status,resolvedAt,resolvedBy}`. Customer contact details are available only in the admin inbox.

Site values are configured with `SITE_SUPPORT_EMAIL`, `SITE_PHONE`, `SITE_WHATSAPP`, `SITE_HOURS`, `SITE_RESPONSE_TIME`, `SITE_ADDRESS`, `SITE_INSTAGRAM`, `SITE_FACEBOOK`, `SITE_TIKTOK`, `SITE_FOUNDER_NAME`, `SITE_FOUNDER_MESSAGE`, `SITE_FOUNDER_PHOTO_URL` (`app.site.*`). Phone and social values default to empty rather than invented contacts. Set the real business address and contacts before deployment. `customersServed` and `cookiesSold` are intentionally null until the order integration. With no approved reviews, `averageRating` is null and `reviewCount` is 0.

### Reviews

| Method | Path | Access / result |
|---|---|---|
| POST | `/api/v1/me/reviews/{cookieId}` | Customer JWT; **201**. `{rating, title, body, authorName}`. Starts `PENDING`; one per customer/cookie, including rejected reviews; duplicates return 409. |
| GET | `/api/v1/cookies/{idOrSlug}/reviews?page=0&size=12&sort=newest` | Public; approved reviews only. `sort=newest\|highest\|lowest`, size 1–48. |
| GET | `/api/v1/reviews/featured?limit=6` | Public; approved ratings 4–5, longest bodies first. Limit 1–12. Archived/deleted cookies excluded. |
| GET | `/api/v1/admin/reviews?status=PENDING\|APPROVED\|REJECTED` | Admin; newest first. Omit status for all. |
| PUT | `/api/v1/admin/reviews/{id}/approve` | Admin; updates status and `moderatedAt`. |
| PUT | `/api/v1/admin/reviews/{id}/reject` | Admin; updates status and `moderatedAt`. |

`ReviewView` = `{id,cookieId,rating,title,body,authorName,verifiedBuyer,status,createdAt,moderatedAt}`. **No customer email is exposed**, including in moderation responses. Rating must be 1–5; title/authorName 1–100 characters; body 1–2000.

Public review page = `{summary:{averageRating,reviewCount,histogram:{"5":n,"4":n,"3":n,"2":n,"1":n}},page:{items:ReviewView[],page,size,totalItems,totalPages}}`. Empty histograms include all five zero buckets. Unknown or archived cookies return 404.

`CatalogService` now populates ratings from approved reviews with one aggregate query per list of cookies, including related-cookie lists. Moderation immediately affects catalog ratings and site statistics.

**Phase 4 integration:** replace `service/engagement/NoPurchaseVerifier` with a `PurchaseVerifier` implementation using paid orders; do not register both as unqualified beans. Current reviews correctly report `verifiedBuyer=false`. Verification happens at submission time; no retrospective re-verification job is included.

### Email system

- Added Thymeleaf and TwelveMonkeys WebP support.
- `EmailService.sendTemplate(to, subject, template, Map<String,Object> model, marketing)` renders HTML and sends MIME `multipart/alternative` with a plain-text part. Existing `send(...)` remains supported.
- Marketing sends require an absolute HTTPS `unsubscribeUrl` without CR/LF; the footer and `List-Unsubscribe: <url>` use it. No marketing email is sent automatically by this phase.
- `MAIL_ENABLED=false` renders and logs metadata without sending or logging reset tokens/message bodies. With mail enabled, SMTP failures are logged; durable retries are not part of this phase.
- Password-reset mail now uses `password-reset`; account/reset-token behavior is unchanged.
- Set `PUBLIC_BASE_URL` (default `http://localhost:8080/guiltFreeCookie`) for absolute images and `FRONTEND_BASE_URL` for shop/account links. Public static assets: `/email/wordmark@2x.png` and `/email/cookie.png`.
- `GET /api/v1/email-assets/cookies/{cookieId}.png` is public; returns a 600×750 opaque cream PNG with a soft shadow, cached under `uploads/email/` with a source-derived filename, ETag and seven-day public cache lifetime. New requests reject unknown/archived cookies or missing images with 404. A changed source produces a new cached file; clients can retain a previously fetched URL for the advertised cache lifetime.

| Method | Path | Result (admin JWT required) |
|---|---|---|
| GET | `/api/v1/admin/email-previews` | Six supported template names. |
| GET | `/api/v1/admin/email-previews/{template}` | Rendered `text/html` with illustrative sample data; `Cache-Control: no-store`. Fetch as text and render with a sandboxed `srcdoc` iframe. |
| POST | `/api/v1/admin/email-previews/{template}/send-test` | **202**; recipient is looked up from the authenticated admin, never supplied by the request. Uses existing mail-enabled configuration. |

Templates: `password-reset`, `contact-auto-reply`, `contact-to-shop`, `order-confirmation`, `order-shipped`, `order-ready-for-pickup`. Unknown template names return 404. See [Phase 6 email models](handoff/phase-6-email-models.md) for the exact order integration contract and reusable fragments.

### Frontend

- `/login`, `/register`, `/forgot-password`, `/reset-password?token=...`, `/account`; separate customer session storage and role-scoped bearer selection. Customer 401s end the customer session. A wrong-current-password 401 is checked against `GET /me` before treating it as expiry.
- `/products` uses paged server search, 300 ms debounced input, category counts, price bounds, in-stock filtering and sorting. Filters are in the URL.
- `/product/{slug}` supports gallery thumbnails, pack availability/pricing, low stock, nutrition, related products, review histogram/list and authenticated review submission. Legacy ID URLs resolve through the API and replace the URL with the slug. Existing cart controls retain their pre-cart behavior.
- Homepage featured selection uses `featured=true`; testimonials and rating counts use approved reviews. Contact and footer use site information. Contact submission now stays on-site with success/error states.
- Admin navigation adds `/admindashboard/inventory` (stock adjustments/stocktake, keyboard-accessible history drawer, pack editor, gallery upload/reorder, details/nutrition), `/messages`, `/reviews`, `/email-previews`. Existing cookie CRUD remains available. The cookie dashboard adds low-stock alerts.
- Prerendering fetches the paged catalog and product details; generated product paths use slugs. As before, without `PRERENDER_API_URL`, product pages render client-side and are omitted from the generated sitemap.

### Validation and integration notes

New `contact_messages` and `reviews` tables are additive. `reviews` has a database unique constraint on `(cookie_id,customer_email)` to enforce duplicate protection under concurrency. Existing dev `ddl-auto=update` creates these tables; production schema management must include them before running with `validate`.

Backend tests cover contact/reviews/site info, actual H2 review queries and uniqueness, public/admin/customer boundaries and rate limiting, complete submission→approval→rejection rating flow, PNG/WebP conversion, MIME and template rendering. `MailpitDeliveryTest` is opt-in (`RUN_MAILPIT_TEST=true`) and sends only to the local SMTP listener at `127.0.0.1:1025`. Six sample emails were captured in Mailpit during verification. Outlook/Gmail client-specific rendering has not been tested; templates include inline styles, font fallbacks and Outlook VML buttons.

`frontend/tests/phase6.browser.cjs` exercises twelve routes at 390px and 1440px, contact submission, slug canonicalization, unavailable packs, iframe sandboxing and separate customer/admin bearer tokens. It uses mocked API fixtures; backend integration is covered independently by the real Spring/H2 tests. Run with Playwright available and Vite on port 5173 (optional `PLAYWRIGHT_MODULE`, `CHROME_PATH`, `TEST_ORIGIN`).

## 2026-10-24 — Phase 3: Cart & discount codes

Guest-first cart. **Store `cartId` in localStorage**; there's no login needed. Every cart endpoint returns the full priced cart (`CartView`), so just re-render from the response. Prices always come from the server.

### Storefront (public; a customer token is optional)

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/api/v1/carts` | – | `201 CartView`. With a customer token it returns their existing cart if there is one. Rate-limited (30/hour per IP) |
| GET | `/api/v1/carts/{cartId}` | – | `404` if it doesn't exist: start a new cart |
| POST | `/api/v1/carts/{cartId}/items` | `{variantId, quantity}` | Adds a pack (`variantId` from product detail). The same pack again increases its quantity |
| PUT | `/api/v1/carts/{cartId}/items/{cartItemId}` | `{quantity}` | `0` removes the item |
| DELETE | `/api/v1/carts/{cartId}/items/{cartItemId}` | – | Remove one item |
| DELETE | `/api/v1/carts/{cartId}/items` | – | Empty the cart (also drops the discount code) |
| POST | `/api/v1/carts/{cartId}/discount` | `{code}` | Case-insensitive. `400` with a message to show ("This code has expired", "Spend R150.00 or more to use this code", …) |
| DELETE | `/api/v1/carts/{cartId}/discount` | – | |
| PUT | `/api/v1/carts/{cartId}/email` | `{email}` | Guests: ask for the email **first** in checkout (it enables cart-recovery emails). `409` for a signed-in customer's cart |

### Signed-in customers (customer token)

| Method | Path | Body | Notes |
|---|---|---|---|
| GET | `/api/v1/me/cart` | – | Their saved cart; `404` if none |
| POST | `/api/v1/me/cart/merge` | `{cartId}` | **Call right after login or registration** with the browser's cartId. It returns the cart to use from now on (it can be a different `cartId`: store the returned one) |

### CartView
```json
{
  "cartId": "…", "status": "ACTIVE", "email": "thandi@example.co.za", "accountOwned": false,
  "items": [
    { "cartItemId": "…", "cookieId": "…", "slug": "double-chocolate-chunk", "name": "Double Chocolate Chunk",
      "image": "cookies/….webp", "variantId": "…", "variantLabel": "Box of 6", "unitsPerPack": 6,
      "unitPrice": 170.00, "quantity": 1, "lineTotal": 170.00, "maxQuantity": 1, "issue": null }
  ],
  "itemCount": 3, "cookieCount": 8,
  "subtotal": 234.00,
  "discount": { "code": "TREAT10", "type": "PERCENT", "description": "10% off your treat", "amount": 23.40,
                "freeShipping": false, "applied": true, "message": null },
  "discountTotal": 23.40,
  "shipping": null,
  "vatIncluded": null,
  "total": 210.60, "currency": "ZAR",
  "canCheckout": true
}
```
- **Quantity selector:** limit it to `maxQuantity`, which already accounts for other pack sizes of the same cookie in the cart (max 50).
- **`issue`** on a line:
  - `NOT_ENOUGH_STOCK`: stock dropped after adding. Show "Only N available", offer to set the quantity to `maxQuantity`, or remove it.
  - `UNAVAILABLE`: the cookie was archived or the pack hidden. It's priced at 0; offer "Remove".
- **`canCheckout: false`** while any line has an issue or the cart is empty. Disable the checkout button.
- **`discount.applied: false`:** the code is still on the cart but isn't counting right now. Show `discount.message` (e.g. the subtotal dropped below the minimum).
- **`shipping`** is `null` until Phase 4: show "Calculated at checkout". `vatIncluded` is `null` unless the shop is VAT-registered.
- **Errors:**
  - `409` when adding more than is in stock. Show `message`, e.g. "Sorry, you can only add 2 more Double Chocolate Chunk cookies" or "… is sold out".
  - `409` when changing a checked-out cart. Start a new cart.
  - `404` for an unknown pack or cookie.
  - `400` for a quantity outside 1–50 or more than 30 different items.

### Admin: discount codes (admin token)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/admin/discounts` | Newest first |
| GET | `/api/v1/admin/discounts/{id}` | |
| POST | `/api/v1/admin/discounts` | See the body below. `409` if the code exists |
| PUT | `/api/v1/admin/discounts/{id}` | Same body; replaces everything except `usedCount` |
| DELETE | `/api/v1/admin/discounts/{id}` | Switches it off (`active: false`); it isn't deleted |

Body: `{code, type, value, minSubtotal?, startsAt?, endsAt?, maxUses?, onePerEmail?, restrictedToEmail?, description?, active?}`
- `type`: `PERCENT` (`value` 1–100), `FIXED` (rand), `FREE_SHIPPING` (no value; it takes effect on shipping in Phase 4)
- `code`: 3–30 letters, numbers, `-` or `_`; stored in upper case
- `maxUses: 1` makes a single-use code. `restrictedToEmail` makes a personal code. Leave the dates empty for "always". Dates are local time (`2026-12-01T00:00:00`)
- A use is counted when an order is paid (Phase 4), not when the code is applied to a cart

### Unchanged
`/cart/*` (admin) still works and now shows the server-computed `totalPrice` of storefront carts.

## 2026-10-24 — Phase 2: Catalog

Use these endpoints for the storefront from now on. `/cookie/getAll` and `/cookie/read/{id}` still work but return the raw entity.

Image paths (`image`) are relative, exactly as before: build URLs with `cookieApi.getImageUrl(image)`. A cookie's **name is `name`** in these responses (it's `description` in the legacy entity).

### Public

**`GET /api/v1/cookies`**: search, filter and sort; paged.

| Query param | Notes |
|---|---|
| `q` | Matches name, long description and ingredients (case-insensitive, max 100 chars) |
| `category` | `LOW_SUGAR`, `DIABETIC_FRIENDLY`, `HIGH_PROTEIN`, `GLUTEN_FREE`, `VEGAN`, `CLASSIC` |
| `minPrice`, `maxPrice` | Unit price in rand |
| `inStock=true` | Hide sold-out cookies |
| `featured=true` | Homepage "featured" row |
| `sort` | `featured` (default: featured first, then A–Z), `price_asc`, `price_desc`, `newest`, `name` |
| `page`, `size` | `page` starts at 0; `size` defaults to 12, max 48 |

Response: `{items: CookieSummary[], page, size, totalItems, totalPages}`

`CookieSummary` = `{cookieId, slug, name, category, image, fromPrice, featured, stockStatus, averageRating, reviewCount}`
- `stockStatus`: `IN_STOCK` | `LOW_STOCK` | `OUT_OF_STOCK`
- `averageRating` is `null` and `reviewCount` is `0` until reviews ship. Hide the stars when the count is 0.

**`GET /api/v1/cookies/{idOrSlug}`**: product detail. Use the slug in URLs (`/product/double-chocolate-chunk`).

```json
{
  "cookieId": "…", "slug": "double-chocolate-chunk", "name": "Double Chocolate Chunk",
  "category": "LOW_SUGAR", "longDescription": "…", "ingredients": "…", "allergies": "…",
  "image": "cookies/….webp",
  "images": [ { "imageId": "…", "image": "cookies/….jpg", "altText": "…", "sortOrder": 0 } ],
  "variants": [
    { "variantId": "…", "label": "Single",    "unitsPerPack": 1,  "price": 34.00,  "pricePerCookie": 34.00, "available": true },
    { "variantId": "…", "label": "Box of 12", "unitsPerPack": 12, "price": 330.00, "pricePerCookie": 27.50, "available": false }
  ],
  "fromPrice": 34.00, "featured": true,
  "stockStatus": "LOW_STOCK", "lowStockRemaining": 8,
  "nutrition": { "weightGrams": 40, "calories": 160, "sugarGrams": 2.5, "proteinGrams": 6.0, "carbsGrams": null, "fatGrams": null },
  "averageRating": null, "reviewCount": 0,
  "related": [ CookieSummary, … up to 4 ]
}
```
- Gallery = `image` (primary) followed by `images[]`.
- Pack picker: show the `variants` and disable the ones with `available: false`. The cart (Phase 3) takes a `variantId`.
- `lowStockRemaining` is only set when the status is `LOW_STOCK` ("Only 8 left").
- Nutrition values are per cookie, and any of them can be `null`: hide what's missing.
- `404` for unknown or archived cookies. Show the 404 page.

**`GET /api/v1/cookies/categories`** returns `[{category, label, count}]` for every category. Labels match `CATEGORY_META.short`.

### Admin (admin token)

The existing admin form keeps using `/cookie/create|update|archive|delete`. The new fields can't be set through `/cookie/*`; use these:

| Method | Path | Body / notes |
|---|---|---|
| GET | `/api/v1/admin/cookies` | All cookies (archived too): `[{cookie, stockStatus, variants, images}]` |
| GET | `/api/v1/admin/cookies/{id}` | One of the above |
| PUT | `/api/v1/admin/cookies/{id}/details` | `{slug?, featured, lowStockThreshold?, weightGrams?, calories?, sugarGrams?, proteinGrams?, carbsGrams?, fatGrams?}`. Replaces all of these; `slug` is kept if omitted |
| PUT | `/api/v1/admin/cookies/{id}/stock` | `{adjustBy: 24}` or `{setTo: 30}`, optional `reason` (`RESTOCK`, `ADJUSTMENT`, `STOCKTAKE`, `DAMAGED`) and `note`. `409` if stock would go below 0 |
| GET | `/api/v1/admin/cookies/{id}/inventory?limit=50` | Stock history: `[{quantityChange, stockAfter, reason, note, actor, createdAt}]` |
| GET | `/api/v1/admin/inventory/low-stock` | Low and sold-out cookies, most urgent first (dashboard widget) |
| GET/POST | `/api/v1/admin/cookies/{id}/variants` | POST `{label, unitsPerPack, price?, active?, sortOrder?}`. Leave `price` empty to charge unit price × pack size |
| PUT | `/api/v1/admin/cookies/{id}/variants/{variantId}` | Same body |
| DELETE | `/api/v1/admin/cookies/{id}/variants/{variantId}` | Hides the pack (`active: false`); it isn't deleted |
| POST | `/api/v1/admin/cookies/{id}/images` | multipart: `image` file and optional `altText`. Max 10 per cookie |
| PUT | `/api/v1/admin/cookies/{id}/images/order` | `{imageIds: [...]}`: every id, in display order |
| DELETE | `/api/v1/admin/cookies/{id}/images/{imageId}` | |

### Behaviour changes
- **Every cookie starts with 0 stock** (shown as `OUT_OF_STOCK`) until stock is set through `/stock`. Existing cookies included.
- Every cookie gets a slug and a "Single" pack automatically (new cookies on create, existing ones on startup).
- A cookie is "low stock" at 12 or fewer, unless it has its own `lowStockThreshold`.
- Hard-deleting a cookie (`/cookie/delete`) also removes its packs and gallery photos.

## 2026-10-24 — Phase 1: Customer accounts

Accounts are optional; guest checkout (Phase 4) never requires one. Customer tokens work exactly like admin tokens: send `Authorization: Bearer <token>`. Store the token the same way the admin session is stored.

**Added (public, rate-limited together: 10 requests / 5 min per IP)**

| Method | Path | Body | Success |
|---|---|---|---|
| POST | `/api/v1/auth/register` | `{email, password, firstName, middleName?, lastName, mobileNumber?, newsletterOptIn?}` | `201 {customer, token}` |
| POST | `/api/v1/auth/login` | `{email, password}` | `200 {customer, token}` |
| POST | `/api/v1/auth/forgot-password` | `{email}` | `204` always (never reveals whether the email exists) |
| POST | `/api/v1/auth/reset-password` | `{token, newPassword}` | `200 {customer, token}` (signs the customer in) |

**Added (customer token required)**

| Method | Path | Body | Success |
|---|---|---|---|
| GET | `/api/v1/me` | – | `200 customer` |
| PUT | `/api/v1/me` | `{firstName, middleName?, lastName, mobileNumber?}` | `200 customer` |
| PUT | `/api/v1/me/password` | `{currentPassword, newPassword}` | `200 {customer, token}` (replace the stored token; other sessions end) |

`customer` = `{customerEmail, name: {firstName, middleName, lastName}, mobileNumber, createdAt}`.

**Errors** use the standard error body. Show `message` to the user:
- `400` validation (bad email, password under 8 characters, missing name, invalid SA mobile number, bad or expired reset link)
- `401` wrong email or password, or wrong current password
- `409` email already registered (the message points the user to "Forgot password")
- `429` too many attempts (`Retry-After` header in seconds)

**Frontend pages needed:** Login, Register, Forgot password, Reset password (reads `?token=` from the emailed link `/reset-password?token=...`), My account (profile and change password).

**Notes**
- Customer tokens are rejected by admin endpoints (`403`), and admin tokens are rejected by `/api/v1/me` (`403`).
- A `401` on `/api/v1/me` means the session ended (password changed elsewhere). Send the user to login.
- `/api/v1/customers` (admin) never returns password data.

## 2026-10-24 — Phase 0: Foundation

**Changed**
- `GET /api/v1/delivery-details/read/{id}` returns `404` when the record doesn't exist (was `200` with an empty body).
- `PUT /api/v1/delivery-details/update` returns `404` for an unknown id (was a silent create).
- `POST /api/v1/delivery-details/create` returns `409` if the id already exists (was a silent overwrite).

**Added**
- Swagger UI and OpenAPI docs, with a bearer-token "Authorize" button.
- New endpoints added from Phase 1 onward return errors in one shape:
  ```json
  { "timestamp": "...", "status": 404, "error": "Not Found", "message": "...", "fieldErrors": [ { "field": "email", "message": "..." } ] }
  ```
  Existing endpoints keep their current responses (empty body or plain-text message).

**No breaking changes** to `/cookie/*`, `/admin/*`, `/newsletter/*`, `/cart/*` or `/api/v1/customers`.
