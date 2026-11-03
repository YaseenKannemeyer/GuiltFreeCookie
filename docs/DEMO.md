# Running and hosting the demo

This is a software demo, not a real bakery. Prices, recipes, allergens, stock and delivery options are sample data. Payments are simulated and email is captured in an admin inbox. Use made-up customer details.

## Local demo

Requirements: Java 25, Node 22.12+ and the installed frontend dependencies (`cd frontend && npm ci`).

From the repository root:

```sh
bash scripts/run-demo.sh
```

- Storefront: http://127.0.0.1:5173
- Admin: http://127.0.0.1:5173/admin
- Local demo login: `owner@example.com` / `DemoCookies123!`
- Discount: `DEMO10` takes 10% off merchandise.
- Email studio: Admin → Email previews. Order confirmations, contact replies and password-reset messages appear in **Demo inbox**. Read them there; no SMTP account is used.
- All 12 products have Single, Box of 6 and Box of 12 variants, with 120 sample cookies initially available per flavour.
- Delivery: sample courier R80, PUDO R60, free delivery from R500, or free collection. These are illustrative settings; no delivery API is called.

The `demo` profile binds the API to the loopback interface and uses an isolated H2 database under `target/demo/`. It never reads or alters the normal MySQL catalogue. Existing demo products and inventory survive restarts and are not overwritten by the seed. The sample admin is created only when the demo database has no admins. The local demo credentials must not be used on a hosted environment.

The frontend defaults to `VITE_DEMO_MODE=true`. This adds demo notices, explains the example policies and blocks handoffs to live PayFast. The payment simulator requires the private order token, rejects expired/cancelled orders and handles repeated payment clicks without charging or taking more stock.

## Verification

With the demo API running:

```sh
cd frontend
npm test
npm run check:demo
npm run build:demo
```

`build:demo` requires a working catalogue, prerenders all 12 products and includes their URLs in the sitemap. It fails rather than silently publishing a product-free sitemap. Set `PRERENDER_API_URL` when the API is not local.

Backend checks:

```sh
./mvnw test
```

An optional SMTP integration test sends only to a local Mailpit instance:

```sh
mailpit --listen 127.0.0.1:8025 --smtp 127.0.0.1:1025
RUN_MAILPIT_TEST=true ./mvnw -Dtest=MailpitDeliveryTest test
```

Demo journeys to try: add a pack, apply DEMO10, choose delivery, simulate payment, view the order and PDF documents, find the confirmation in the inbox, update the order in admin and record a demo refund. Cancelling before payment restores the cart and stock. Cancelling a paid order before processing restores stock and records a simulated refund.

## Vercel and Azure

Follow [HOSTING-PLAN.md](HOSTING-PLAN.md) for the account resources. The committed configuration implements the frontend headers and clean URLs described there, with these demo-specific adjustments:

1. On Azure use `SPRING_PROFILES_ACTIVE=prod,showcase`. The `showcase` profile uses the production MySQL connection, secrets, HTTPS and persistent uploads, but seeds sample products, simulates payment and captures email. Do **not** combine `prod` with the local `demo` profile: startup refuses that combination.
2. Supply `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, a strong `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `FRONTEND_BASE_URL`, `PUBLIC_BASE_URL` and `UPLOAD_DIR=/home/uploads`. URLs must use the real Azure and Vercel addresses. Azure's `SERVER_PORT` is supported; an explicit `PORT` takes precedence.
3. Supply your own `ADMIN_BOOTSTRAP_EMAIL` and strong `ADMIN_BOOTSTRAP_PASSWORD` for the first hosted start; remove them after the first login. No hosted default admin credentials are supplied. Use `JPA_DDL_AUTO=update` once to create the schema (including `email_deliveries`), then remove it so `validate` is used. Back up an existing database before a schema update.
4. Set Vercel Root Directory to `frontend`, and configure `VITE_DEMO_MODE=true`, `VITE_SITE_URL`, `VITE_API_BASE_URL` and `PRERENDER_API_URL`. The two API variables must point directly at Azure's `/guiltFreeCookie`, as required by the hosting plan. Deploy the API first. `frontend/vercel.json` uses the strict demo build and deliberately has no catch-all or API rewrite. `cleanUrls=true` serves `/about` from `about.html`.
5. GitHub's API workflow tests and packages automatically. Set the repository variable `AZURE_WEBAPP_NAME` and secret `AZURE_WEBAPP_PUBLISH_PROFILE`, then use **Run workflow** to deploy through the `production` environment. Until configured and manually run, it only verifies the API. The frontend workflow runs payment tests and a compilation build; Vercel's strict build checks the live catalogue.
6. Enable Always On and point Azure's health check at `/guiltFreeCookie/actuator/health/readiness`. Uptime monitors can watch `/guiltFreeCookie/actuator/health` and `/guiltFreeCookie/actuator/health/dependencies`. Readiness includes the database; dependencies includes the email outbox and SMTP when enabled. A failed email does not fail readiness. All health responses hide internal details.
7. Enable database backups in Azure and keep uploads on the persistent `/home` path. These are account settings and have not been configured by local code. The delivery provider remains simulated; no provider credentials, shipment booking or speculative provider health request is added.

No cloud resources have been created or deployed by these changes. Credentials and account setup remain in your own Azure, Vercel and GitHub settings. The hosting plan's cost figures are estimates, not an enforced spending limit.

## Email reliability outside demo mode

Transactional emails are stored in `email_deliveries` before dispatch. SMTP failures retry up to five times, with delays of 1, 5, 15 and 60 minutes. Admin → Email studio lists recent deliveries and can retry failed, unexpired messages. A password-reset email is never sent after its reset window. Message bodies are cleared after delivery or expiry; metadata is removed after seven days. SMTP delivery is at least once: a connection failure after SMTP acceptance can lead to a duplicate.

Marketing journeys retain their existing consent checks and flow retry schedule. SMTP errors now propagate to the runner, so a failed marketing email does not silently advance the flow. In demo/showcase modes, messages stay in the captured inbox instead.

Before ever using this as a real shop, replace sample business facts, verify recipes and nutrition, review the actual policies, set `VITE_DEMO_MODE=false`, remove showcase/demo profiles, and configure and test real providers. None of those approvals is implied by a successful demo test.

References: [PayFast hosted endpoints](https://developers.payfast.co.za/), [Vercel clean URLs](https://vercel.com/docs/project-configuration/vercel-json#cleanurls), [Spring Boot health groups](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html).
