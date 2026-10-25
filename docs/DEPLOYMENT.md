# Deploying GuiltFreeCookie

The shop has two parts:

- **API:** Spring Boot (Java 25), served under `/guiltFreeCookie`, backed by MySQL.
- **Storefront and admin:** Vue, built to static files. Product pages are prerendered for search engines.

Put both on one domain: serve the static files and proxy `/guiltFreeCookie` to the API. Requests are then same-origin, so CORS never gets in the way.

```
https://guiltfreecookie.co.za/                    → frontend/dist (static files; unknown paths get 404.html)
https://guiltfreecookie.co.za/guiltFreeCookie/…   → Spring Boot on :8080
```

## 1. Before you go live

| | What | Where to get it |
|---|---|---|
| ☐ | A domain with HTTPS | Your host. HTTPS terminates at the proxy; set `REQUIRE_HTTPS=true` |
| ☐ | MySQL 8 database and user | Your host |
| ☐ | PayFast **live** merchant ID, key and passphrase | PayFast dashboard → Settings → Integration. Set a passphrase there first |
| ☐ | A transactional email service | Brevo, Amazon SES, Mailgun or similar. Add its SPF and DKIM records to your domain, or mail lands in spam |
| ☐ | Your legal details for the checkout page | Registered business name, registration number and full physical address (ECTA s43). `SITE_ADDRESS` holds the address |
| ☐ | Recipes, allergens and prices confirmed by the owner | Admin → Cookies |
| ☐ | Legal check on the "Diabetic friendly" category name | R146 forbids implying a food manages a disease |

## 2. Environment variables (API)

Run with `SPRING_PROFILES_ACTIVE=prod`. The prod profile has no fallbacks for secrets: anything marked **required** stops the app from starting if it's missing.

**Core**

| Variable | Required | Notes |
|---|---|---|
| `DB_URL` | yes | e.g. `jdbc:mysql://db:3306/guiltfreecookie` |
| `DB_USERNAME`, `DB_PASSWORD` | yes | |
| `JWT_SECRET` | yes | Base64, at least 32 bytes: `openssl rand -base64 32`. Changing it signs everyone out |
| `CORS_ALLOWED_ORIGINS` | yes | Your site's origin, e.g. `https://guiltfreecookie.co.za` |
| `FRONTEND_BASE_URL` | yes | The same origin. Used in every email link |
| `PUBLIC_BASE_URL` | yes | Public address of the API, e.g. `https://guiltfreecookie.co.za/guiltFreeCookie`. Used for email images, tracking and the PayFast notify URL |
| `UPLOAD_DIR` | recommended | Where cookie photos are stored. **Put it on a persistent volume**, or uploads vanish on redeploy |
| `PORT` | no | Defaults to 8080 |
| `REQUIRE_HTTPS` | no | Defaults to `true` in prod |
| `JWT_EXPIRATION_MINUTES` | no | Defaults to 480 in prod |
| `JPA_DDL_AUTO` | no | Defaults to `validate`. See "Database" below |

**PayFast**

| Variable | Required | Notes |
|---|---|---|
| `PAYFAST_SANDBOX` | yes | `false` for live payments |
| `PAYFAST_MERCHANT_ID`, `PAYFAST_MERCHANT_KEY`, `PAYFAST_PASSPHRASE` | yes | The **live** values. The passphrase must match the dashboard exactly |
| `PAYFAST_NOTIFY_URL` | no | Defaults to `PUBLIC_BASE_URL` + `/api/v1/payments/payfast/notify`. PayFast must be able to reach it |

**Email**

| Variable | Required | Notes |
|---|---|---|
| `MAIL_HOST`, `MAIL_PORT` | yes | From your email service |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | yes | From your email service |
| `MAIL_SMTP_AUTH`, `MAIL_STARTTLS` | yes | Usually both `true` |
| `MAIL_FROM` | recommended | e.g. `GuiltFreeCookie <hello@guiltfreecookie.co.za>`. Must be a domain you've verified with the service |
| `MAIL_ENABLED` | no | Defaults to `true` in prod |
| `FLOWS_ENABLED` | no | `false` starts with every automated email flow switched off. Admins can switch them on at Admin → Email flows |

**Shop details** (shown on the site and in emails)

| Variable | Notes |
|---|---|
| `SITE_SUPPORT_EMAIL`, `SITE_PHONE`, `SITE_WHATSAPP`, `SITE_HOURS`, `SITE_RESPONSE_TIME` | Contact page, footer, checkout |
| `SITE_ADDRESS` | Full physical address (shown before payment, and in every email footer) |
| `SITE_INSTAGRAM`, `SITE_FACEBOOK`, `SITE_TIKTOK` | Full URLs |
| `SITE_FOUNDER_NAME`, `SITE_FOUNDER_MESSAGE`, `SITE_FOUNDER_PHOTO_URL` | The founder note in order and welcome emails |
| `VAT_REGISTERED` | `true` shows the VAT portion on receipts |
| `SHIPPING_COURIER_RATE`, `SHIPPING_PUDO_RATE`, `SHIPPING_FREE_THRESHOLD`, `SHIPPING_PICKUP_ADDRESS`, `SHIPPING_PICKUP_INSTRUCTIONS` | Only the first-run defaults. After that, admins change them at Admin → Delivery |

**Optional link secrets.** Each defaults to `JWT_SECRET`. Set one only to rotate that kind of link on its own:

| Variable | Rotating it breaks |
|---|---|
| `ORDER_LINK_SECRET` | Guest order-tracking links in past emails |
| `EMAIL_LINK_SECRET` | Unsubscribe links in past emails |
| `ACCOUNT_CLAIM_SECRET` | Outstanding "add my earlier orders" links (they last 24 hours anyway) |

`ADMIN_BOOTSTRAP_EMAIL` and `ADMIN_BOOTSTRAP_PASSWORD` create the first admin on an empty database (see step 6).

`API_DOCS_ENABLED=true` turns Swagger UI on in prod. Leave it off on the public site.

## 3. Frontend build

Copy `frontend/.env.example` to `frontend/.env.production.local` and set `VITE_SITE_URL`. Point `PRERENDER_API_URL` at a running API so product pages are prerendered. Then:

```
cd frontend && npm ci && npm run build
```

Serve `frontend/dist` over HTTPS, and answer unknown paths with `404.html`. That page also starts the app, so private pages like `/order/…` and `/admindashboard` still load, while search engines see a 404. Add the security headers listed in the README at the host.

## 4. Database

Production uses `ddl-auto=validate`: the app checks the schema but never changes it.

- **First deploy, empty database:** start once with `JPA_DDL_AUTO=update` so the tables are created, then remove it.
- **Upgrading an existing database:** run the SQL listed under "Action required" in each new `docs/API-CHANGELOG.md` entry, or run once with `JPA_DDL_AUTO=update` after taking a backup. Hibernate adds new tables and columns, but it never changes or drops existing ones.
- **Back up daily.** The orders, payments and consent records (newsletter opt-ins and unsubscribes) are your legal record.

## 5. Health check

`GET /guiltFreeCookie/actuator/health` answers `{"status":"UP"}` when the app and database are up. It's public, isn't rate-limited, and reveals nothing else. Mail isn't part of it, so an email-provider outage doesn't make your host restart the shop. Nothing else under `/actuator` is exposed.

## 6. After deploying

1. **First admin:** on an empty database, set `ADMIN_BOOTSTRAP_EMAIL` and `ADMIN_BOOTSTRAP_PASSWORD` (12+ characters) for the first start. Sign in at `/admin`, change the password (Admin → profile), then remove both variables. They do nothing once any admin exists.
2. Admin → Delivery: check rates, the pickup address and the free-delivery threshold.
3. Admin → Email flows: switch on the flows you want and check the discount sizes. Admin → Email previews: send yourself a test of each.
4. Place a real order for a single cookie and pay with a real card. Check that it turns **Paid**, the confirmation email arrives, and stock goes down. Then record a refund in admin and refund it in the PayFast dashboard.
5. Unsubscribe from a test email using its link, and check you get nothing more.
