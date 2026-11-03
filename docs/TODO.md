# What's left

The code is ready for hosting: `frontend/vercel.json`, `.github/workflows/deploy-api.yml`, and the health endpoints are all in place. Everything below is either account setup or work that hasn't been built yet.

Hosted target: **Vercel** for the storefront and **Azure for Students** for the API and MySQL. It costs $0 cash; the API uses about $13/month of student credit.

## 1. Hosting

### Azure MySQL
- [ ] Create an **Azure Database for MySQL Flexible Server**: Burstable B1MS, MySQL 8, 32 GB, in the same region as the App Service.
- [ ] Networking: public access, tick **Allow public access from any Azure service**.
- [ ] Create database `guiltfreecookie` and a user with rights on that schema only (not `root`).
- [ ] Set server parameter `time_zone` to `+02:00`.
- [ ] Turn on automated backups.

### Azure App Service
- [ ] Create a Web App: Code, **Java 25 (Java SE)**, Linux, **Basic B1** (not F1, which sleeps and stops the scheduled jobs). If Java 25 isn't listed, check `az webapp list-runtimes --os linux`.
- [ ] General settings: **Always On** on.
- [ ] Health check path: `/guiltFreeCookie/actuator/health/readiness`.
- [ ] Application settings:

```
SPRING_PROFILES_ACTIVE=prod,showcase
DB_URL=jdbc:mysql://<server>.mysql.database.azure.com:3306/guiltfreecookie?sslMode=REQUIRED&serverTimezone=Africa/Johannesburg
DB_USERNAME=<user>
DB_PASSWORD=<password>
JWT_SECRET=<openssl rand -base64 32>
CORS_ALLOWED_ORIGINS=https://<project>.vercel.app
FRONTEND_BASE_URL=https://<project>.vercel.app
PUBLIC_BASE_URL=https://<app>.azurewebsites.net/guiltFreeCookie
UPLOAD_DIR=/home/uploads
ADMIN_BOOTSTRAP_EMAIL=<first admin email>
ADMIN_BOOTSTRAP_PASSWORD=<12+ characters>
JPA_DDL_AUTO=update
```

`showcase` keeps the sample products, simulated payments and captured email inbox. Never combine `prod` with the local `demo` profile; startup refuses it. Azure sets `SERVER_PORT` itself, so `PORT` isn't needed.

### GitHub
- [ ] App Service → Overview → **Download publish profile**. If it's greyed out, turn on **SCM Basic Auth Publishing Credentials**.
- [ ] Repo → Settings → Secrets and variables → Actions: secret `AZURE_WEBAPP_PUBLISH_PROFILE`, variable `AZURE_WEBAPP_NAME`.
- [ ] Merge to `main`, then Actions → **Verify and deploy API** → **Run workflow**. Deploys only run by hand.

### Vercel (deploy the API first, because the build reads the catalogue)
- [ ] Import the repo, Root Directory `frontend`.
- [ ] Environment variables:

```
VITE_DEMO_MODE=true
VITE_SITE_URL=https://<project>.vercel.app
VITE_API_BASE_URL=https://<app>.azurewebsites.net/guiltFreeCookie
PRERENDER_API_URL=https://<app>.azurewebsites.net/guiltFreeCookie
```

Both API URLs must point straight at Azure, not through a Vercel rewrite. Behind a proxy, PayFast notifications fail the IP check and every visitor shares one rate limit.

### After the first start
- [ ] `/guiltFreeCookie/actuator/health` returns `{"status":"UP"}`.
- [ ] Sign in at `/admin`, change the password, then remove `JPA_DDL_AUTO` and both `ADMIN_BOOTSTRAP_*` settings and restart.
- [ ] Check the headers on [securityheaders.com](https://securityheaders.com).
- [ ] Run a demo order end to end: pay, see it in the demo inbox, check that stock goes down.
- [ ] Upload a cookie photo, restart the App Service, and check that the photo still shows.
- [ ] Azure cost alert at $15/month (Cost Management → Budgets).

## 2. Monitoring

- [ ] **Better Stack** (free): three uptime monitors at 3-minute intervals
  - Storefront: `https://<project>.vercel.app/`
  - API: `…/guiltFreeCookie/actuator/health`
  - Dependencies: `…/guiltFreeCookie/actuator/health/dependencies` (mail and outbox)
- [ ] Optional status page, plus email and push alerts in the Better Stack app.
- [ ] **Not built: heartbeats.** Add a small `Heartbeat` component that GETs a URL (3 s timeout, logs on failure, does nothing when the URL is blank). Ping it as the last line of `ReservationReleaseJob` and of the daily cron in `FlowTriggers`. Settings: `HEARTBEAT_RESERVATIONS_URL` and `HEARTBEAT_DAILY_FLOWS_URL`.
- [ ] **Not built: Sentry** (free Developer plan).
  - Backend: add the Sentry starter for **Spring Boot 4** and set `SENTRY_DSN`, `SENTRY_ENVIRONMENT=production` and `SENTRY_SEND_DEFAULT_PII=false`.
  - Frontend: add `@sentry/vue`, initialise it in `main.js` only when `VITE_SENTRY_DSN` is set, and add Sentry's ingest origin to `connect-src` in `vite.config.js`.
  - Keep PII off (POPIA).

## 3. Real email (only needed when leaving the captured demo inbox)

- [ ] Get a domain (free through the GitHub Student Developer Pack) and point it at Vercel.
- [ ] **Brevo** (free, 300 emails/day): verify the domain (SPF, DKIM, DMARC), add sender `hello@<domain>`, generate an SMTP key, and turn off Brevo's open and click tracking (the app tracks these itself, and Brevo's link rewriting would wrap the unsubscribe links).
- [ ] Settings: `MAIL_HOST=smtp-relay.brevo.com`, `MAIL_PORT=587`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH=true`, `MAIL_STARTTLS=true`, `MAIL_FROM`, `SITE_SUPPORT_EMAIL`, `FLOWS_ENABLED=false` to start with.
- [ ] Check with [mail-tester.com](https://www.mail-tester.com) (aim for 9/10 or better) and confirm Gmail's **Show original** shows SPF, DKIM and DMARC all as `PASS`.

## 4. Live courier rates: ShipLogic (The Courier Guy). Not built

Delivery is currently flat-rate and simulated. When it's wanted:
- [ ] Get sandbox credentials through [thecourierguy.co.za/integrations](https://thecourierguy.co.za/integrations) and read the [ShipLogic API docs](https://www.shiplogic.com/api-docs).
- [ ] Add box sizes and weights to `ShippingSettings` (ShipLogic won't quote without them).
- [ ] `ShipLogicClient` + `ShipLogicShippingProvider`. Widen `ShippingRateProvider.quotes` to take an address and cookie count. Fall back to the flat rate on an error or after 3 s. Cache quotes for 10 minutes. Lock the quoted price on the order.
- [ ] Admin **Book courier** button: create the shipment once, save the tracking number and URL, mark the order Shipped, offer a waybill download. Only book Monday to Wednesday.
- [ ] `POST /api/v1/delivery/webhook` with a constant-time secret check. Move orders to Shipped or Delivered, never backwards, and always answer 200.
- [ ] A `delivery` health indicator, added to the `dependencies` group.
- [ ] Open question: can ShipLogic quote and book PUDO lockers?

## 5. Before it's ever a real shop

- [ ] The owner confirms the recipes, allergens and prices for all 12 cookies, including the chocolate suppliers' allergens and cross-contact. Don't invent nutrition values.
- [ ] Review the 11 generated cookie images (only Double Chocolate Chunk was approved individually).
- [ ] Get a legal check on the "Diabetic friendly" category name (R146: no implied disease claims).
- [ ] Registered business name, registration number and physical address (`SITE_ADDRESS`, required by ECTA s43), plus the rest of the `SITE_*` contact details.
- [ ] Review the privacy, terms, refund and cookie policies.
- [ ] PayFast **live** merchant ID, key and passphrase; set `PAYFAST_SANDBOX=false`.
- [ ] Remove the `showcase` profile and set `VITE_DEMO_MODE=false`.
- [ ] Real-card test order, then a refund recorded in admin and paid back in the PayFast dashboard. Test unsubscribing from a marketing email.

## 6. Smaller loose ends

- [ ] Test the cinematic hero video's scroll performance on a real phone. Its browser integration was never checked on mobile.
