# 🍪 GuiltFreeCookie

An ecommerce shop for a small-batch, sugar-free cookie bakery in Cape Town: a **Vue 3** storefront (prerendered to static HTML) and a **Spring Boot 4** API on **Java 25**, backed by **MySQL**.

- Storefront: [guiltfreecookie.me](https://guiltfreecookie.me)
- Status page: [status.guiltfreecookie.me](https://status.guiltfreecookie.me)
- Swagger UI (development): `/guiltFreeCookie/swagger-ui.html`

Products and the bakery address are sample data, and payments and courier bookings run against the PayFast and ShipLogic **sandboxes**, so no real money moves and no real courier is booked.

## Contents

1. [What it does](#what-it-does)
2. [Tech stack](#tech-stack)
3. [Architecture (domain-driven design)](#architecture-domain-driven-design)
4. [Run it locally](#run-it-locally)
5. [Configuration](#configuration)
6. [Security](#security)
7. [Email](#email)
8. [Payments, shipping and address search](#payments-shipping-and-address-search)
9. [API reference](#api-reference)
10. [CI/CD: how the GitHub pipeline works](#cicd-how-the-github-pipeline-works)
11. [Hosting, monitoring and operations](#hosting-monitoring-and-operations)
12. [Testing](#testing)
13. [What has been built](#what-has-been-built)
14. [Further documentation](#further-documentation)

---

## What it does

**Customers**
- Browse the catalogue by category (diabetic-friendly, gluten-free and more), search, see ratings and reviews.
- Choose a pack size (single, box and so on), add to cart as a guest or signed in, apply a discount code.
- Check out with a **courier** (live ShipLogic quotes), a **PUDO locker** or **collection**, with address search that suggests South African addresses as you type.
- Pay through PayFast, then get an order confirmation, a receipt and an invoice (PDF), and later shipping and delivery emails.
- Create an account: order history, address book, saved cart that merges on sign-in, change password, forgot password, and claiming earlier guest orders.
- Leave a verified-purchase review through a link in the post-purchase email.

**Admin**
- Orders: change status, add tracking, **book a courier**, download the waybill, refund.
- Catalogue: edit details, pack variants and prices, photos (upload and reorder), stock and the inventory log, low-stock list.
- Discount codes, shipping settings (rates, free-delivery threshold, PUDO switch), review moderation, contact inbox.
- Analytics overview, email delivery log with retry, email flow switches and statistics, email template previews with test sends.
- Admin accounts: create, deactivate, reset and change password.

**Rules built into the shop**
- Minimum order of R300 (subtotal before discounts and delivery), free delivery from R750.
- Stock is **reserved** at checkout and released by a job every 5 minutes if the order isn't paid in time.
- A payment that arrives for an already-cancelled order leaves the order cancelled, takes no stock and sends the shop a "refund needed" email.

---

## Tech stack

| Layer | Technology |
|---|---|
| Language and runtime | Java 25 |
| API framework | Spring Boot 4.1 (Spring MVC, Spring Data JPA / Hibernate, Bean Validation) |
| Security | Spring Security, JWT (jjwt), BCrypt |
| Database | MySQL 8 (Azure Database for MySQL); H2 in the local demo profile |
| Email | Spring Mail, Thymeleaf templates, Brevo SMTP |
| PDFs | OpenHTMLtoPDF (receipts, invoices) |
| Images | ImageIO WebP plugin |
| API docs | springdoc-openapi (Swagger UI) |
| Monitoring | Spring Actuator, Micrometer OTLP and OpenTelemetry to Grafana Cloud, Sentry, Better Stack |
| Build | Maven (wrapper included) |
| Frontend | Vue 3, Vue Router, Vite, Tailwind CSS, Axios, GSAP |
| Frontend tests | Node's built-in test runner, a browser responsive check |
| CI/CD | GitHub Actions |
| Hosting | Azure App Service (API), Azure MySQL, Vercel (storefront and DNS) |
| Third parties | PayFast (payments), ShipLogic / The Courier Guy (courier), Google Places (address search), Brevo (sending email), ImprovMX (receiving email) |

---

## Architecture (domain-driven design)

The API follows a layered, domain-driven structure under `src/main/java/za/ac/cput/guiltfreecookie`:

| Package | Role |
|---|---|
| `domain` | Entities and value objects: `Cookie`, `CookieVariant`, `Customer`, `Admin`, `Cart`, `CartItem`, `CustomerOrder`, `OrderItem`, `Payment`, `DiscountCode`, `DeliveryDetails`, `Review`, `InventoryMovement`, `EmailDelivery`, `EmailSend`, `FlowEnrollment` and their enums (`OrderStatus`, `ShippingMethod`, `CookieCategory`, ...) |
| `factory` | Builds and validates entities: `CookieFactory`, `CustomerFactory`, `AdminFactory`, `CartFactory`, `AddressFactory`, `DeliveryDetailsFactory` |
| `repository` | Spring Data JPA repositories over the relational database (one per aggregate) |
| `service` | Business rules, grouped by area: `cart` (pricing), `catalog`, `order` (checkout, reservations, courier booking), `payment`, `shipping`, `notification` (email), `flow` (lifecycle email), `engagement` (contact, reviews), `account`, `analytics`, `discount`, `monitoring` |
| `controller` | REST controllers returning JSON |
| `security` | `JwtService`, `JwtAuthenticationFilter`, `RateLimitFilter`, `ClientAddressFilter`, `CurrentUser` |
| `config` | `SecurityConfig`, CORS, OpenAPI, health indicators, admin bootstrap, demo data |
| `dto`, `exception`, `util` | Request/response shapes, `GlobalExceptionHandler`, helpers |

The database has 24 tables (customers, admins, cookies, variants, images, carts, orders, order items, order status events, payments, discount codes, delivery details, inventory movements, reviews, contact messages, newsletter subscribers, shipping settings and the email tables).

---

## Run it locally

Requires Java 25 and Node 22.12+.

### Demo mode (no MySQL, email or payment account needed)

```sh
cd frontend && npm ci && cd ..
bash scripts/run-demo.sh
```

- Storefront: http://127.0.0.1:5173
- Admin: http://127.0.0.1:5173/admin (the demo admin is created on first start; ask the project owner for the login)
- Discount code `DEMO10` takes 10% off
- Emails show up under **Admin → Email previews → Demo inbox**
- Payment goes through a simulated gateway at `/demo-payment`

The `demo` profile uses an isolated H2 database under `target/demo/` and never touches MySQL.

### Against MySQL

```sh
cp .env.example .env          # fill in DB_* and JWT_SECRET (openssl rand -base64 32)
./mvnw spring-boot:run
cd frontend && npm run dev     # proxies /guiltFreeCookie to :8080
```

To load the packaged database instead of letting Hibernate create the schema:

```sh
mysql -u root -p < guiltfreecookie_schema_and_seed.sql
```

### Helper scripts

| Script | What it does |
|---|---|
| `scripts/run-demo.sh` | Starts the API in demo mode and the Vite dev server |
| `scripts/restock.sh` | Sets stock on every visible cookie (staging or production) |
| `scripts/reprice.sh` | Resets the four pack prices on every visible cookie; safe to run twice |

---

## Configuration

All secrets come from environment variables. Nothing secret is committed; `.env.example` and `frontend/.env.example` list the names.

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection |
| `JWT_SECRET`, `JWT_EXPIRATION_MINUTES` | Token signing key; lifetime (default 1440 = 24 hours) |
| `ORDER_LINK_SECRET`, `EMAIL_LINK_SECRET`, `ACCOUNT_CLAIM_SECRET` | Sign guest order links, unsubscribe links and claim-order links (default to the JWT secret; set them to rotate separately) |
| `ADMIN_BOOTSTRAP_EMAIL`, `ADMIN_BOOTSTRAP_PASSWORD` | Creates the first admin when the admins table is empty |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS` | SMTP |
| `PAYFAST_*` | Merchant id, key, passphrase, sandbox switch |
| `SHIPLOGIC_API_KEY`, `SHIPLOGIC_COLLECTION_*` | Courier quotes, bookings, collection address and contact |
| `GOOGLE_PLACES_API_KEY` | Address search (blank hides the search box) |
| `ORDER_MINIMUM_SUBTOTAL`, `SHIPPING_FREE_THRESHOLD`, `SHIPPING_PUDO_ENABLED` | Shop rules |
| `REQUIRE_HTTPS`, `API_DOCS_ENABLED` | Production hardening switches |
| `OTLP_*`, `SENTRY_DSN`, `HEARTBEAT_*_URL` | Metrics, error reporting, heartbeats |
| `JPA_DDL_AUTO` | `update` locally, `validate` on staging and production |

Spring profiles: `demo` (H2, no external services), `prod` (production settings), `showcase` (sandbox payments and courier, flows off).

---

## Security

**Authentication**
- Customers and admins sign in with email and password and receive a signed **JWT** (HS256, 24-hour lifetime, carrying the role and a token version so a password change invalidates older tokens).
- Passwords are hashed with **BCrypt**; they are never stored or logged in plain text.
- The API is **stateless** (no server sessions). Password reset uses single-use, expiring tokens sent by email (valid for 60 minutes).

**Authorisation (checked on the server)**
- `SecurityConfig` maps every request to a rule. Public: catalogue, cart, checkout, order lookup by signed link, auth, contact, newsletter, health, email tracking and unsubscribe. Customer-only: everything under `/api/v1/me/**` (`ROLE_CUSTOMER`). Everything else, including every admin endpoint, requires `ROLE_ADMIN`.
- The router's admin check in the frontend only hides pages; nothing in the browser decides who is an admin.
- Guest order pages are protected by an unguessable, signed access token rather than an account.

**Abuse protection**
- `RateLimitFilter`: per-IP fixed-window limits, for example 10 tries per 5 minutes on admin and customer sign-in, 5 newsletter sign-ups and 5 contact messages per hour, 30 carts and 20 checkouts per hour.
- `ClientAddressFilter` strips the port from the forwarded client address so limits and the PayFast check see the real client behind Azure.
- Request validation with Bean Validation on every input; `GlobalExceptionHandler` returns safe error bodies.

**Payments**
- PayFast notifications are verified three ways: the signature, the amount, and the source IP against PayFast's published ranges and its resolved hostnames. The notification is also confirmed back with PayFast's validate endpoint.

**Transport and headers**
- HTTPS only in production, HSTS for one year including subdomains.
- API headers: `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy` denying camera, microphone, geolocation and payment, `X-Content-Type-Options`, `X-Frame-Options`.
- Storefront headers (Vercel): a Content-Security-Policy header, `frame-ancestors 'none'` and `Cross-Origin-Opener-Policy`; securityheaders.com grade **A**.
- CORS only allows the configured storefront origins and the `Authorization` and `Content-Type` headers.
- CSRF protection is off on purpose: the API has no cookie the browser sends by itself, and only trusts an `Authorization` header that the storefront's own script adds.

**Privacy and compliance**
- Privacy policy, terms, refund policy and cookie policy pages.
- Separate, unticked marketing consent at sign-up and checkout; one-click unsubscribe (RFC 8058 `List-Unsubscribe`) and a preferences page.
- Sentry runs with personal data, session replay and tracing switched off. No analytics or advertising trackers are used.
- The courier only receives the delivery area for quotes, not the street.

---

## Email

Mail is sent through **Brevo** SMTP from `hello@guiltfreecookie.me`, with SPF, DKIM and DMARC passing (mail-tester 8.6/10). Replies to `hello@` are forwarded by **ImprovMX**. In demo mode, nothing leaves the machine: messages are captured in an admin inbox.

**How sending works**
- `EmailService` builds each message from a Thymeleaf template; `EmailOutbox` queues it in the database and sends it in the background, so a slow or failed SMTP call never blocks an order. The admin **Email deliveries** page shows each message's status and lets an admin retry a failed one.
- `EmailSuppression` keeps unsubscribed and bounced addresses from being mailed.
- Opens and clicks are tracked through a pixel and redirect links; links are signed so they can't be forged.
- Health checks (`mail` and `outbox`) feed the dependencies monitor.

**Transactional email**

| Email | When |
|---|---|
| Order confirmation | Payment received |
| Order shipped (with tracking) | Admin ships or books the courier |
| Ready for collection | Collection orders |
| Order cancelled | Customer cancels |
| Password reset | Forgot password |
| Claim earlier orders | Customer links earlier guest orders to their account |
| Contact auto-reply and contact to shop | Contact form |
| Order cancelled to shop; paid after cancel to shop ("refund needed") | Shop alerts |
| Resubscribe confirmation | Preferences page |

**Lifecycle (marketing) flows**: `WELCOME`, `BROWSE_ABANDONMENT`, `CART_ABANDONMENT`, `POST_PURCHASE`, `REPLENISHMENT`, `WINBACK` and `SUNSET`. Each can be switched on or off by an admin, only goes to subscribers who opted in, runs from a daily 09:00 job, and has its own statistics (sent, opened, clicked). The `showcase` profile turns the daily job off.

Admins can preview every template and send a test from **Admin → Email previews**.

---

## Payments, shipping and address search

- **PayFast** (sandbox): redirect checkout and an instant payment notification endpoint. The demo gateway replaces it locally.
- **ShipLogic / The Courier Guy**: live courier quotes by delivery area, courier booking from the admin order page, waybill PDF download, and an automatic check every 30 minutes that moves shipped orders to Delivered. A flat-rate provider is the fallback.
- **PUDO lockers** and **collection** are alternative shipping methods (PUDO can be switched off in Admin → Delivery).
- **Google Places** (Places API New): address suggestions at checkout. Only that one API is enabled on the key.

---

## API reference

Base path: `/guiltFreeCookie`. Auth column: **Public** needs no token, **Customer** needs a customer JWT, **Admin** needs an admin JWT (`Authorization: Bearer <token>`). Interactive docs: `/guiltFreeCookie/swagger-ui.html`.

### Authentication (public)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/auth/register` | Create a customer account, returns customer and token |
| POST | `/api/v1/auth/login` | Customer sign in |
| POST | `/api/v1/auth/forgot-password` | Email a reset link |
| POST | `/api/v1/auth/reset-password` | Set a new password with the emailed token |
| POST | `/admin/login` | Admin sign in |

### Catalogue (public)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/cookies` | List visible cookies |
| GET | `/api/v1/cookies/categories` | Categories |
| GET | `/api/v1/cookies/{idOrSlug}` | Cookie detail with pack variants |
| GET | `/api/v1/cookies/{idOrSlug}/reviews` | Approved reviews |
| GET | `/api/v1/reviews/featured` | Featured reviews |
| GET | `/api/v1/site/info` | Contact details, hours, shipping text |
| GET | `/api/v1/address-search`, `/api/v1/address-search/place` | Address suggestions and place details |
| GET | `/api/v1/email-assets/cookies/{cookieId}.png` | Email-safe product image |

### Cart, checkout and orders (public, signed by cart id or order access token)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/carts` | Create a cart |
| GET | `/api/v1/carts/{cartId}` | Read a cart |
| GET | `/api/v1/carts/{cartId}/shipping-options` | Quotes for courier, PUDO, collection |
| POST | `/api/v1/carts/{cartId}/items` | Add an item (`variantId`, `quantity`) |
| PUT | `/api/v1/carts/{cartId}/items/{cartItemId}` | Change quantity |
| DELETE | `/api/v1/carts/{cartId}/items/{cartItemId}` | Remove an item |
| DELETE | `/api/v1/carts/{cartId}/items` | Empty the cart |
| POST / DELETE | `/api/v1/carts/{cartId}/discount` | Apply or remove a discount code |
| PUT | `/api/v1/carts/{cartId}/email` | Attach an email to the cart |
| GET | `/api/v1/carts/recover/{token}` | Recover an abandoned cart from an email link |
| POST | `/api/v1/checkout` | Turn a cart into an order |
| GET | `/api/v1/orders/{orderNumber}` | Order status (needs the order's access token) |
| GET | `/api/v1/orders/{orderNumber}/receipt`, `/invoice` | PDF documents |
| POST | `/api/v1/orders/{orderNumber}/payment` | Start payment (PayFast form fields) |
| POST | `/api/v1/orders/{orderNumber}/demo-payment` | Simulated payment (demo only) |
| POST | `/api/v1/orders/{orderNumber}/cancel` | Cancel an unpaid order |
| POST | `/api/v1/orders/{orderNumber}/cancel-paid` | Cancel a paid order |
| POST | `/api/v1/orders/{orderNumber}/reorder` | Rebuild a cart from a past order |
| POST | `/api/v1/payments/payfast/notify` | PayFast server notification (IP, signature and amount verified) |

### Customer account (Customer)

| Method | Path | Purpose |
|---|---|---|
| GET / PUT | `/api/v1/me` | Read or update the profile |
| PUT | `/api/v1/me/password` | Change password |
| GET | `/api/v1/me/orders` | Order history |
| GET | `/api/v1/me/orders/{orderNumber}` | One order |
| GET | `/api/v1/me/orders/claimable` | Guest orders that can be linked |
| POST | `/api/v1/me/orders/claim`, `/claim/confirm` | Start and confirm linking by email |
| GET | `/api/v1/me/cart` | Saved cart |
| POST | `/api/v1/me/cart/merge` | Merge the guest cart on sign-in |
| GET / POST | `/api/v1/me/addresses` | List or add addresses |
| PUT / DELETE | `/api/v1/me/addresses/{id}` | Update or delete |
| PUT | `/api/v1/me/addresses/{id}/default` | Make default |
| POST | `/api/v1/me/reviews/{cookieId}` | Review a cookie you bought |

### Contact, newsletter and email links (public)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/contact` | Send a message |
| POST | `/newsletter/subscribe` | Subscribe |
| GET | `/api/v1/email/o/{sendId}.gif` | Open tracking pixel |
| GET | `/api/v1/email/c/{sendId}` | Click redirect |
| POST | `/api/v1/email/unsubscribe`, `/unsubscribe/one-click`, `/resubscribe`, `/stay` | Preferences |
| POST | `/api/v1/reviews/by-link` | Review from an emailed link |
| POST | `/api/v1/events/product-view` | Product view event for browse-abandonment emails |

### Admin: orders, catalogue, discounts, shipping (Admin)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/admin/orders`, `/{orderNumber}` | List and read orders |
| PUT | `/api/v1/admin/orders/{orderNumber}/status` | Change status |
| PUT | `/api/v1/admin/orders/{orderNumber}/tracking` | Save tracking |
| POST | `/api/v1/admin/orders/{orderNumber}/courier-booking` | Book the courier |
| GET | `/api/v1/admin/orders/{orderNumber}/waybill` | Waybill PDF |
| POST | `/api/v1/admin/orders/{orderNumber}/refund` | Refund |
| GET | `/api/v1/admin/cookies`, `/{cookieId}` | List and read cookies |
| PUT | `/api/v1/admin/cookies/{cookieId}/details` | Edit details |
| GET / POST | `/api/v1/admin/cookies/{cookieId}/variants` | List or add pack variants |
| PUT / DELETE | `/api/v1/admin/cookies/{cookieId}/variants/{variantId}` | Edit or remove a variant |
| POST | `/api/v1/admin/cookies/{cookieId}/images` | Upload a photo |
| PUT | `/api/v1/admin/cookies/{cookieId}/images/order` | Reorder photos |
| DELETE | `/api/v1/admin/cookies/{cookieId}/images/{imageId}` | Delete a photo |
| PUT | `/api/v1/admin/cookies/{cookieId}/stock` | Adjust stock |
| GET | `/api/v1/admin/cookies/{cookieId}/inventory` | Stock movements |
| GET | `/api/v1/admin/inventory/low-stock` | Low-stock list |
| GET / POST | `/api/v1/admin/discounts` | List or create codes |
| GET / PUT / DELETE | `/api/v1/admin/discounts/{discountId}` | Read, update, delete |
| GET / PUT | `/api/v1/admin/shipping-settings` | Rates, free-delivery threshold, PUDO |
| GET | `/api/v1/admin/analytics` | Dashboard figures |

### Admin: reviews, messages, email (Admin)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/admin/reviews` | Reviews waiting |
| PUT | `/api/v1/admin/reviews/{reviewId}/approve`, `/reject` | Moderate |
| GET | `/api/v1/admin/messages` | Contact inbox |
| PUT | `/api/v1/admin/messages/{messageId}/resolve` | Mark resolved |
| GET | `/api/v1/admin/email-deliveries` | Delivery log |
| POST | `/api/v1/admin/email-deliveries/{id}/retry` | Retry a send |
| GET | `/api/v1/admin/email-deliveries/{id}/preview` | Demo inbox preview |
| GET / PUT | `/api/v1/admin/email-flows` | Flow switches |
| GET | `/api/v1/admin/email-flows/stats` | Flow statistics |
| GET | `/api/v1/admin/email-previews`, `/{template}` | Template previews |
| POST | `/api/v1/admin/email-previews/{template}/send-test` | Send a test email |

### Admin accounts (Admin)

| Method | Path | Purpose |
|---|---|---|
| POST | `/admin/create` | Create an admin |
| GET | `/admin/getAll`, `/admin/read/{id}` | List and read |
| PUT | `/admin/update` | Update |
| DELETE | `/admin/delete/{id}` | Delete |
| PUT | `/admin/active/{id}` | Activate or deactivate |
| POST | `/admin/resetPassword/{id}` | Reset a password |
| PUT | `/admin/changePassword/{id}` | Change a password |

### Original CRUD endpoints (from the first iterations; Admin unless noted)

| Group | Paths |
|---|---|
| Cookie | `GET /cookie/getAll` (public), `GET /cookie/read/{cookieId}` (public), `GET /cookie/getAllForAdmin`, `POST /cookie/create`, `PUT /cookie/update/{cookieId}`, `DELETE /cookie/delete/{cookieId}`, `PUT /cookie/archive/{cookieId}`, `PUT /cookie/unarchive/{cookieId}` |
| Customer | `POST /api/v1/customers`, `GET /api/v1/customers`, `GET /api/v1/customers/{email}`, `PUT /api/v1/customers`, `DELETE /api/v1/customers/{email}` |
| Cart | `POST /cart/create`, `GET /cart/read/{id}`, `PUT /cart/update`, `DELETE /cart/delete/{id}`, `GET /cart/getAll`, `GET /cart/customer/{customerEmail}`, `GET /cart/status/{status}` |
| Delivery details | `POST /api/v1/delivery-details/create`, `GET .../all`, `GET .../read/{id}`, `PUT .../update`, `DELETE .../delete/{id}` |
| Newsletter | `GET /newsletter/subscribers` |

### Operations

| Method | Path | Purpose |
|---|---|---|
| GET | `/actuator/health`, `/actuator/health/readiness` | Liveness and readiness (public) |
| GET | `/actuator/health/dependencies` | Mail, outbox and courier checks, used by the uptime monitor |

A Postman collection is in [postman/](postman).

---

## CI/CD: how the GitHub pipeline works

Two GitHub Actions workflows live in [.github/workflows/](.github/workflows). Full detail is in [docs/WORKFLOW.md](docs/WORKFLOW.md).

```
feature branch ──PR──▶ staging ──PR──▶ main
                         │               │
        tests + auto-deploy to staging   tests only; production is deployed by hand
```

**1. `deploy-api.yml`: Verify and deploy API**
- **Triggers:** a push or pull request touching `src/**`, `pom.xml`, the product images or the workflow itself; or a manual run (`workflow_dispatch`) where you choose `staging` or `production`.
- **`verify` job** (every trigger): checks out the code, installs Temurin Java 25 with the Maven cache, runs `./mvnw -B verify` (all tests plus packaging) and uploads the JAR as an artifact.
- **`deploy` job** (needs `verify`): runs for a **push to `staging`** (deploys to staging) or a **manual run**. A manual production deploy is refused unless it runs from `main`.
- It targets the GitHub **environment** `staging` or `production`. The environment holds the variable `AZURE_WEBAPP_NAME` and the secret `AZURE_WEBAPP_PUBLISH_PROFILE`; if the app name isn't set, the deploy is skipped with a warning instead of failing.
- `azure/webapps-deploy` then pushes the JAR to the Azure App Service. Jobs queue per environment (`concurrency`), so two deploys never overlap.

**2. `frontend.yml`: Verify storefront**
- **Triggers:** a push or pull request touching `frontend/**`.
- Installs Node 22, runs `npm ci`, `npm test` and `npm run build` (which also prerenders the pages). It does not deploy: **Vercel** builds and deploys the storefront on its own from the connected repository (`staging` branch to a preview URL, `main` to production). During the build the prerender step reads the catalogue from the API and retries for up to 10 minutes, so a merge that also deploys the API still builds.

**Branch rules**
- `main` is protected by a ruleset: pull request required, one approval, stale approvals dismissed, no force push, no deletion.
- Production API deploys are manual and only from `main`; staging deploys on every push to `staging`.

---

## Hosting, monitoring and operations

| Piece | Where |
|---|---|
| Storefront | Vercel (`guiltfreecookie.me`, `www` redirects to the apex; DNS on Vercel) |
| API | Azure App Service, Java 25, Always On, HTTPS only, TLS 1.2, FTP off; one app for staging, one for production |
| Database | Azure Database for MySQL, separate staging and production databases, backups on, `ddl-auto=validate` |
| Uploads | Product photos are written to the folder set by `UPLOAD_DIR` and served from `/uploads/**` |

**Monitoring**
- **Better Stack:** uptime monitors for the storefront, the API health and the dependencies group; heartbeats for the 5-minute reservation job and the daily flow job; a public status page at [status.guiltfreecookie.me](https://status.guiltfreecookie.me).
- **Sentry:** error reporting for the API and the web app (EU region, no personal data).
- **Grafana Cloud:** the API pushes request, error, JVM, database-pool and shop metrics over OTLP every minute; a dashboard JSON is in [docs/grafana/](docs/grafana). Azure metrics are read through a read-only identity.
- **Actuator:** health, readiness and a dependencies group.

**Operational notes**
- The API takes about 3 to 4.5 minutes to start on the current plan, so the container start limit is raised to 600 seconds. Never restart or deploy staging and production at the same time (they share one plan and one core).
- Domain `guiltfreecookie.me` was claimed through the GitHub Student Developer Pack.

---

## Testing

```sh
./mvnw test                    # 77 backend test classes
cd frontend && npm test        # frontend unit tests
cd frontend && npm run test:responsive   # browser responsive check
```

Backend tests cover the order lifecycle (including late payment for a cancelled order), PayFast verification and signatures, the client-address filter, ShipLogic quoting and booking, address search, pricing, stock rules, security rules and the email flows. [docs/SMOKE-TEST.md](docs/SMOKE-TEST.md) is a manual checklist for a release.

---

## What has been built

A summary of the work delivered, grouped by area.

**Shop and accounts**
- Catalogue with categories, search, ratings and reviews; four pack sizes per cookie plus a Box of 24; photo management.
- Guest and signed-in carts that merge on sign-in; discount codes; minimum order and free-delivery rules.
- Checkout with order numbers, stock reservation and release, cancellation, refunds, reorder, receipts and invoices as PDFs.
- Customer accounts with order history, address book, password reset and claiming guest orders; admin accounts with activate, deactivate and reset.

**Payments and delivery**
- PayFast sandbox end to end (redirect, notification, Paid status), with a fix for notifications being rejected behind Azure's proxy and a safe path for late payments.
- ShipLogic live courier rates, courier booking, waybill download, automatic delivery tracking, a courier health check, and tracking privacy settings.
- Google Places address search at checkout.

**Email**
- Brevo sending with authenticated domain, ImprovMX receiving, queued delivery with admin retry, suppression list, tracking, unsubscribe and preferences.
- Order emails, shop alerts and seven lifecycle flows with admin switches and statistics; template previews and test sends.

**Security and privacy**
- JWT authentication, role-based authorisation, BCrypt, rate limits, signed links, validated payment notifications, security headers on API and storefront, HTTPS and HSTS.
- Privacy, terms, refund and cookie policy pages; marketing consent and one-click unsubscribe.

**Operations**
- GitHub Actions verify and deploy pipeline with staging and production environments.
- Azure and Vercel hosting, custom domain, status page, Better Stack, Sentry and Grafana Cloud dashboards, heartbeat checks, helper scripts for restocking and repricing.
- Storefront prerendered for search engines, with a sitemap and page metadata.

**Still open (hardening backlog)**
- Move login tokens from browser storage to an `HttpOnly` cookie with CSRF protection; two-step sign-in for admins; per-account lockout; leaked-password check.
- Terms acceptance records, data download and account deletion, retention limits, subprocessor list.
- Emails for every order step (processing, delivered, refunded, payment failed), admin password reset by email, discount-code emails.
- Security documents (`AUTH.md`, `SECURITY.md`, `THREAT-MODEL.md`), accessibility pass, query and caching optimisation, Google Search Console and SEO tooling.

---

## Further documentation

| File | What it covers |
|---|---|
| [docs/WORKFLOW.md](docs/WORKFLOW.md) | How a change gets from a laptop to production |
| [docs/SMOKE-TEST.md](docs/SMOKE-TEST.md) | Release checklist |
| [docs/COSTS.md](docs/COSTS.md) | Running costs of each service |
| [docs/PACKAGES.md](docs/PACKAGES.md) | Offering this stack to other businesses |
| [docs/grafana/](docs/grafana) | Grafana dashboard JSON |

## License

[MIT](LICENSE)
