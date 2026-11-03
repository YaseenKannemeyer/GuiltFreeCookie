# GuiltFreeCookie API: how it fits together

Start here. For the request and response of each endpoint, use Swagger UI at `/guiltFreeCookie/swagger-ui.html` (on in development). `docs/API-CHANGELOG.md` has the details and the reasons behind them, newest first. Deployment is covered in `docs/DEPLOYMENT.md`.

Everything lives under `/guiltFreeCookie`. New endpoints are under `/api/v1`. The older `/cookie/*`, `/admin/*` and `/newsletter/*` routes still work and are used by the admin cookie grid and the footer.

## Who can call what

| Caller | How they prove it | Can reach |
|---|---|---|
| Anyone | nothing | Catalogue, cart, checkout, contact, site info, reviews (read), newsletter, email links |
| Guest with an order | the order's **token** (`accessToken` in the checkout response, and in every order email link) | `GET /api/v1/orders/{n}?token=` plus pay-again, cancel, reorder and review-by-link |
| Customer | `Authorization: Bearer` from `POST /api/v1/auth/login` | `/api/v1/me/**`: profile, password, cart, orders, addresses, claiming guest orders |
| Admin | `Authorization: Bearer` from `POST /admin/login` | Everything else, including `/api/v1/admin/**` |

Tokens are JWTs (HS256, `JWT_SECRET`). Changing a password, a reset or deactivation ends every session for that person straight away (a version number in the token). Order, unsubscribe and claim links are HMAC-signed, not stored, so they can't be listed or guessed.

Anything not explicitly public in `config/SecurityConfig.java` needs an admin. **A new public endpoint must be added there**, and a new public POST should get a rule in `security/RateLimitFilter.java`.

## Buying a cookie, step by step

```
Product page ─ POST /carts, POST /carts/{id}/items ──► cart (id kept in localStorage)
Checkout     ─ PUT /carts/{id}/email (guests)        ──► abandoned-cart reminders become possible
             ─ GET /carts/{id}/shipping-options, GET /carts/{id}?shippingMethod=
             ─ POST /checkout ──► order PENDING_PAYMENT, stock held 30 min, {orderNumber, accessToken, payment}
Browser      ─ auto-submits the form to PayFast's hosted page (card details never touch us)
PayFast      ─ POST /payments/payfast/notify (ITN) ──► signature, source IP, amount and server checks ──► PAID
             ─ emails go out after the transaction commits: confirmation now, flows later
Shopper      ─ returns to /order/{n}?token=  (the page polls until PAID; the receipt prints; invoice and receipt PDFs unlock)
Admin        ─ PROCESSING ─► SHIPPED (tracking emailed) or READY_FOR_PICKUP ─► DELIVERED (starts post-purchase emails)
```

`service/cart/PricingService` is the **only** place totals are calculated. The cart, the checkout page and the order all use it, so the price shown is always the price charged. Prices are VAT-inclusive rand. Stock is counted in single cookies; a "Box of 6" uses 6.

An unpaid order releases its stock after 30 minutes (`ReservationReleaseJob`). A cancel from PayFast's page releases it straight away and reopens the cart.

## Things that run on their own

| Job | When | Does |
|---|---|---|
| `ReservationReleaseJob` | every 5 min | Cancels unpaid orders past their hold and returns their stock |
| `FlowTriggers.sendDue` | every 5 min | Sends due email-flow steps |
| `FlowTriggers.findAbandonedCarts` | every 15 min | Starts cart reminders for carts idle for an hour |
| `FlowTriggers.daily` | 09:00 shop time | Starts win-back and "still want our emails?" flows |
| `AdminBootstrap` | startup | Creates the first admin on an empty database, if configured |

How the email flows behave (consent, pacing, quiet hours, exits) is described in the Phase 7 entry of the changelog.

## Errors

New endpoints answer errors as JSON:

```json
{ "timestamp": "…", "status": 409, "error": "Conflict", "message": "Sorry, Lemon Almond is sold out", "fieldErrors": [] }
```

`message` is written for shoppers and can be shown as is. Codes: `400` invalid input, `401` not signed in, `403` signed in but not allowed, `404` not found (also used for someone else's order or address, so nothing leaks), `409` the state doesn't allow it (sold out, already paid), `429` rate limited (with `Retry-After`).

## Pages and the endpoints behind them

| Page | Endpoints |
|---|---|
| Home | `/cookies?featured=true`, `/reviews/featured`, `/site/info`, `/newsletter/subscribe` |
| Shop, search | `/cookies?q=&category=&sort=&page=`, `/cookies/categories` |
| Product | `/cookies/{slug}`, `/cookies/{id}/reviews`, `/events/product-view` |
| Cart, checkout | `/carts/*`, `/checkout`, `/me/addresses` (signed in) |
| Order and tracking | `/orders/{n}?token=`, `/orders/{n}/payment`, `/orders/{n}/cancel`, `/orders/{n}/cancel-paid`, `/orders/{n}/invoice?token=`, `/orders/{n}/receipt?token=` |
| Account | `/auth/*`, `/me`, `/me/orders`, `/me/addresses`, `/me/orders/claim*` |
| Email links | `/carts/recover/{token}`, `/orders/{n}/reorder`, `/reviews/by-link`, `/email/*` |
| Contact | `/contact`, `/site/info` |
| Admin | `/api/v1/admin/{analytics,orders,shipping-settings,email-flows,email-previews,discounts,reviews,messages,cookies,inventory}`, plus `/cookie/*` and `/admin/*` |

## Code map

| Package | Holds |
|---|---|
| `domain` | JPA entities, built with a `Builder` and `copy()`; no setters |
| `controller` | REST endpoints with Swagger annotations |
| `service/cart`, `service/order`, `service/payment` | Cart, pricing, checkout, orders, PayFast |
| `service/flow` | Email flows: enrolment, runner, copy, consent, tracking |
| `service/account` | Customer order history, address book, claiming guest orders |
| `service/analytics` | The admin overview |
| `service/notification` | Spring Mail and Thymeleaf templates (`templates/email/`) |
| `security`, `config` | JWT filter, rate limits, security rules, bootstrap |

Tests: run `./mvnw test`. They use H2, need no database or mail server, and cover the rules above end to end.
