# Handoff: Phase 5, customer accounts (ChatGPT, backend + frontend)

You're the second engineer on GuiltFreeCookie, a South African small-batch cookie shop. Claude is building Phase 7 (automated email flows) on `Completing-the-site` at the same time. Your scope is below; it does not depend on Claude's work.

**Where to work:** the folder `../guiltFreeCookie-phase5` on branch `phase-5-chatgpt`. It was created from `Completing-the-site` at `218aa18`. Phases 0–4 and 6 are done. Don't commit to `Completing-the-site`; the user merges your branch when you're done.

Read first:
- `docs/API-CHANGELOG.md` (the API contract; the Phase 4 entry covers orders)
- `PRODUCT.md` (who the shoppers are; legal and claims rules)
- `docs/handoff/phase-6-chatgpt.md`, the "Stack and conventions" section only. Those rules still apply: Builder entities with no setters, `Helper` validation, boxed types in request records, few comments, Swagger annotations, and a changelog entry with every change.

---

## Goal

A signed-in customer can:
- see every order they've placed;
- keep an address book and use it at checkout;
- pull in orders they placed as a guest before they had an account, safely.

What already exists, so don't rebuild it:
- **Customer auth:** `POST /api/v1/auth/register|login|forgot-password|reset-password`.
- **Profile and password:** `GET|PUT /api/v1/me` and `PUT /api/v1/me/password` (`AccountController`).
- **Security:** `/api/v1/me/**` is already `hasRole("CUSTOMER")`. In a controller, `@AuthenticationPrincipal String email` is the customer's email.
- **Order fields:** `CustomerOrder` has `customerEmail` and `accountOwned`. Checkout sets `accountOwned = true` when the shopper was signed in.
- **Order view:** `OrderViews.view(order)` builds the full order view the order page uses.
- **Order links:** `OrderLinks.token(order)` signs the guest link token used by `/order/{orderNumber}?token=…`.
- **Frontend:**
  - `frontend/src/views/AccountView.vue` (profile and password)
  - `composables/useCustomerAuth.js`
  - `views/OrderView.vue` (order tracking page; it needs `?token=`)
  - `views/CheckoutView.vue`, which already pre-fills name, email and phone from the account

---

## A. Order history (backend)

- `GET /api/v1/me/orders?page=&size=` returns the customer's orders, newest first, paged with the existing `dto/catalog/PageResponse` (`{items, page, size, totalItems, totalPages}`), the same shape as `AdminOrderService.search`.
  - Include orders where `customerEmail = me` and `accountOwned = true`.
  - Leave out `PENDING_PAYMENT` orders older than their reservation, and `CANCELLED` orders that were never paid. Those are abandoned PayFast attempts, not orders the customer thinks they placed.
  - Each item: `orderNumber`, `createdAt`, `status`, `shippingMethod`, `itemCount`, `total`, the first item's cookie image (for a thumbnail) and **`token`** (`OrderLinks.token(order)`). The frontend links straight to the existing order page with that token, so `OrderView.vue` needs no changes.
- `GET /api/v1/me/orders/{orderNumber}` returns `OrderViews.view(order)` plus `token`, and 404s for an order that isn't this customer's. Never reveal that the order exists.
- Put this in a new `MyOrderController` and `MyOrderService`, not in `AccountController` or `OrderService`.

## B. Address book (backend)

Extend `domain/DeliveryDetails` **additively** into a saved address. Existing fields stay; existing `DeliveryDetailsController` routes keep working.
- **New fields:**
  - `customerEmail`
  - `label` (e.g. "Home", "Work", max 30)
  - `recipientName`
  - `phone` (SA mobile, validated with `Helper.isValidMobileNumber`)
  - `isDefault`
  - `createdAt`
- **Routes:** `GET|POST /api/v1/me/addresses`, `PUT|DELETE /api/v1/me/addresses/{id}`, and `PUT /api/v1/me/addresses/{id}/default`.
- **Rules:**
  - A customer can only see or change their own addresses; anyone else's is a 404.
  - Maximum 10 addresses per customer.
  - The first address becomes the default.
  - Setting a default clears the old one.
  - Deleting the default promotes the newest remaining address.
- **Address format:** the same as checkout's `AddressRequest`: streetNumber, streetName, complex, suburb, city, postalCode (4 digits, a **String**), province.
- **Checkout stays untouched** on the backend. The frontend copies a saved address into the existing checkout form fields. Don't change `CheckoutService` or `CheckoutRequest`.

## C. Claim guest orders (backend)

Someone could register with another person's email address, and **no email verification exists**. So claiming must prove the customer controls the inbox:
- `GET /api/v1/me/orders/claimable` returns `{count}`: paid orders with `customerEmail = me` and `accountOwned = false`. It's a count only, with no order details.
- `POST /api/v1/me/orders/claim` emails a signed claim link to the account's own email. The link is HMAC over email + expiry, 24h, the same style as `OrderLinks`. Use its own secret property, `app.account.claim-secret`, with a dev default in `application.properties` and the test properties.
  - Rate-limit it: add a rule in `security/RateLimitFilter`, about 3 per hour.
  - The email uses the branded template system (`EmailService.sendTemplate`, a new `templates/email/claim-orders.html` built from the existing fragments, plus a case in `EmailTemplates.sample(...)` so admins can preview it).
- `POST /api/v1/me/orders/claim/confirm {token}` checks the signature, the expiry and that the token's email equals the signed-in customer. Then it sets `accountOwned = true` on those orders and returns `{claimed}`.
- `CustomerOrder` has no setters; use its Builder `copy()` pattern.

## D. Frontend

Match the existing site exactly:
- **Fonts:** Luckiest Guy (`font-display`) for **one** headline per screen, Nunito for everything else.
- **Colours:** cream ground (`#E3D9C8`), chocolate text, caramel (`bg-primary`) accents.
- **Components:** `BaseButton`, `BaseInput`, the `.field` class, `QuantitySelector`. Tailwind's 1rem is 10px here.
- **Rules:**
  - Every screen works at 390px wide with no sideways scrolling. Grid children with inputs need `min-w-0`.
  - Status badges come from `src/constants/orderStatus.js`.
  - Money uses `useFormat().rand`.
  - Copy is short and plain.

Build:

1. **Account page** (`AccountView.vue`): add **Orders** and **Addresses** sections above "Your details".
   - On mobile, show one column with in-page anchors; on desktop, a left section nav.
   - **Orders:** rows with thumbnail, order number, date, status badge and total, each linking to `/order/{orderNumber}?token={token}`. Include an empty state with a "Browse cookies" button and a "Load more" button.
   - When `claimable.count > 0`, show a banner: "We found N earlier orders placed with this email." with a "Send me a link" button, then "Check your inbox".
2. **Claim route:** `/account/claim?token=…` calls confirm, then shows "N orders added to your account" with a link back to Orders. If the customer isn't signed in, send them to sign-in and back.
3. **Address book UI:**
   - A list of cards with label, a default badge, and Edit / Delete / Make default.
   - Add and edit happen inline, in the same field layout as the checkout delivery step (copy it from `CheckoutView.vue`).
   - Delete asks for confirmation inline, not with `window.confirm`.
4. **Checkout delivery step:** when signed in and saved addresses exist, show them as choice tiles (`components/checkout/ChoiceTile.vue`) above the address form. Picking one fills the form; the default is pre-selected. Under the form, a "Save this address to my account" checkbox (off by default) calls `POST /api/v1/me/addresses` just before the order is placed. A failure there must never block payment.
   - **This is the only change allowed in `CheckoutView.vue`. Keep it small.**
5. Put the API calls in a **new** `src/api/accountApi.js`, and don't edit `storeApi.js`. Customer token handling: add `/api/v1/me` to the rules in `authInterceptors.js` if it isn't already covered. Check first.

## Shared files (Claude edits these too, so keep your edits small and additive)

- **Backend:**
  - `config/SecurityConfig.java`: you shouldn't need it.
  - `security/RateLimitFilter.java`: add one rule.
  - `application*.properties` and test properties: append a section.
  - `service/notification/EmailTemplates.java`: add one case to `sample(...)` and the template list.
- **Frontend:**
  - `router/routes.js`: append routes.
  - `api/authInterceptors.js`: only if needed.
  - `CheckoutView.vue`: only item 4.

**Don't touch:**
- Cart, newsletter, discount, `CustomerAuthService`, `OrderEmailListener`, or anything under `service/order` except reading it. Claude's Phase 7 changes those.
- `.impeccable/`.

## Done means

- `./mvnw test` passes with new tests:
  - Mockito tests for `MyOrderService`, the address service and the claim service, covering the ownership 404s, the 10-address cap, default promotion, and an expired, tampered or other-email claim token;
  - `@DataJpaTest` for the order-history query;
  - `SecurityConfigTest` cases showing `/api/v1/me/orders` and `/api/v1/me/addresses` reject anonymous and admin tokens.
- `cd frontend && npm run build` passes.
- Swagger annotations on the new controllers. A dated `docs/API-CHANGELOG.md` entry at the top ("Phase 5: Customer accounts"), including any manual SQL needed: `ddl-auto=update` adds the new `delivery_details` columns, but list them anyway.
- **Manual check:**
  1. Register, place a paid order (PayFast sandbox) and see it in Orders.
  2. Place a guest order with a second email, register with that email, claim it from the emailed link (Mailpit) and see it appear.
  3. Save two addresses, pick one at checkout, and check it all at 390px wide.
- Leave everything uncommitted or commit on `phase-5-chatgpt`. Never commit to `Completing-the-site`.
