# What's left

The code is ready for hosting: `frontend/vercel.json`, `.github/workflows/deploy-api.yml`, and the health endpoints are all in place. Everything below is either account setup or work that hasn't been built yet.

Hosted target: **Vercel** for the storefront and **Azure for Students** for the API and MySQL. It costs $0 cash; the API uses about $13/month of student credit.

## 1. Hosting

Set up on 3 November 2026. Storefront on **Vercel**, API and MySQL on **Azure for Students** (South Africa North). Both environments run `prod,showcase`.

| | Staging | Production |
|---|---|---|
| Storefront | `guilt-free-cooki-git-7d1aa2-mogamat-yaseen-kannemeyers-projects.vercel.app` (Vercel shortened the branch URL; it stays fixed) | `guilt-free-cookie.vercel.app` |
| API (App Service) | `guiltfreecookie-api-staging.azurewebsites.net/guiltFreeCookie` | `guiltfreecookie-api.azurewebsites.net/guiltFreeCookie` |
| Database | `guiltfreecookie_staging`, user `gfc_staging` | `guiltfreecookie`, user `gfc_app` |
| Deploys | Every push to `staging` | By hand from `main` (Actions → Verify and deploy API → `production`) |

Shared: resource group `rg-guiltfreecookie`, MySQL Flexible Server `gfc-mysql` (Burstable B1ms, 32 GiB, 396 IOPS, `time_zone=+02:00`, 7-day backups), App Service plan `asp-guiltfreecookie` (Linux B1), Vercel project `guilt-free-cookie`, budget `gfc-monthly-15` ($15/month alerts). Database passwords are kept by the owner, not in the repo. The MySQL firewall only allows Azure services; add your own IP temporarily to connect from a laptop.

Staging holds a copy of the local development database (test orders included). Production holds only the catalogue: 20 cookies, their variants, the 3 admins, shipping and email-flow settings, and the `DEMO10` and `12R12` codes.

### Fixed: PayFast payment notifications were rejected

Resolved on staging on 4 November 2026: a sandbox payment turned the order Paid, and PayFast logged the notification (`2027707`, 07:19:39) as `Completed · Success`. Before the fix, the order never became Paid, PayFast showed `Pending QUEUE · cURL Error`, and the API answered 400:

```
PayFast notification rejected: 144.126.193.139:46794 isn't a PayFast address
```

- [x] **Strip the port from the client address.** `ClientAddressFilter` runs before everything else and turns `ip:port` (and `[ipv6]:port`) into the bare address, so the PayFast check and `RateLimitFilter` both see the real client.
- [x] **Check the notification IP against PayFast's published ranges** as well as the IPs its hostnames resolve to: `197.97.145.144/28`, `41.74.179.192/27`, `102.216.36.0/28`, `102.216.36.128/28`, `144.126.193.139` (`PayFastVerifier.PAYFAST_RANGES`). Compare against [PayFast's list](https://developers.payfast.co.za/docs#ports-ips) now and then.
- [x] Tests: `ClientAddressFilterTest`, `PayFastVerifierTest`.
- [x] Deployed to staging (`4b8d8ff`) and ran a sandbox payment end to end: redirect, payment, notification and Paid status all work.
- [x] **Late payment for a cancelled order.** If the customer or an admin cancelled it, the order now stays Cancelled with no stock taken. A history note is added and the shop gets one "Refund needed" email (`order-paid-after-cancel-to-shop`), even when PayFast repeats the notification. Orders cancelled only because the payment window ran out are still honoured as Paid, as before.
- [x] The two old queued sandbox notifications (`GFC-261103-VC2D` and the 21:28 order) were cancelled by the owner, so they won't arrive. The late-payment behaviour is covered by `OrderLifecycleTest`.

Already found and fixed on the way:
- PayFast's public sandbox merchant `10000100` rejects every signature, with or without its documented passphrase. Both apps use the owner's own sandbox account instead (`PAYFAST_MERCHANT_ID`, `PAYFAST_MERCHANT_KEY`, `PAYFAST_PASSPHRASE`). `showcase` keeps sandbox mode forced on.
- `DEMO_SIMULATED_PAYMENTS=false` (set on both apps) sends demo checkouts to the PayFast sandbox instead of the simulated payment page. Without it, `showcase` uses the simulated page.

### Still to do
- [x] **Stock:** all 12 visible cookies restocked to 50 on staging and production with `scripts/restock.sh` (4 November 2026).
- [x] **GitHub `production` environment:** secret `AZURE_WEBAPP_PUBLISH_PROFILE` and variable `AZURE_WEBAPP_NAME=guiltfreecookie-api` are saved, with deployments limited to `main` (checked 4 November 2026; the variable had a stray line break, now fixed).
- [x] Merged `staging` into `main` (PR #43) and deployed the API to production (4 November 2026). Health, readiness and dependencies are `UP`.
- [x] Redeployed Vercel production once the API answered: `guilt-free-cookie.vercel.app` serves the prerendered catalogue (20 sitemap entries) against the production API, with security headers and a CSP meta tag in place.
- [x] Storefront headers on securityheaders.com: **A** (4 November 2026). It only missed CSP because the policy was a `<meta>` tag; `vercel.json` now also sends it as a header, plus `frame-ancestors 'none'` and `Cross-Origin-Opener-Policy`. Rescanned after deploying: **A+** (4 November 2026).
- [x] Production: admin sign-in and a sandbox payment end to end, checked by the owner (4 November 2026).
- [ ] Rotate the `gfc_staging` database password and update `DB_PASSWORD` on the staging app. (`gfcadmin` and `gfc_app` were rotated on 3 November.)
- [x] Staging's container logging turned off again (4 November 2026) now that PayFast works.

### Done
- [x] MySQL server, both databases and users, `time_zone`, backups.
- [x] Both App Services: Java 25, Always On, HTTPS only, TLS 1.2, FTP off, health check `/guiltFreeCookie/actuator/health/readiness`, `JAVA_OPTS=-Xmx512m`.
- [x] GitHub `staging` environment and the first staging deploy.
- [x] Vercel project with Preview variables for the `staging` branch and Production variables.
- [x] Staging: health `UP`, admin sign-in, `JPA_DDL_AUTO` removed (starts with `validate`), photos survive a restart, API security headers, cost alert.

### Reference: app settings

Production (staging differs only where noted):

```
SPRING_PROFILES_ACTIVE=prod,showcase
DB_URL=jdbc:mysql://gfc-mysql.mysql.database.azure.com:3306/guiltfreecookie?sslMode=REQUIRED&serverTimezone=Africa/Johannesburg   # staging: guiltfreecookie_staging
DB_USERNAME=gfc_app                       # staging: gfc_staging
DB_PASSWORD=<password>
JWT_SECRET=<openssl rand -base64 32>      # different on each app
CORS_ALLOWED_ORIGINS=https://guilt-free-cookie.vercel.app      # staging: the staging storefront URL
FRONTEND_BASE_URL=https://guilt-free-cookie.vercel.app         # staging: the staging storefront URL
PUBLIC_BASE_URL=https://guiltfreecookie-api.azurewebsites.net/guiltFreeCookie
UPLOAD_DIR=/home/uploads
JAVA_OPTS=-Xmx512m
DEMO_SIMULATED_PAYMENTS=false
PAYFAST_SANDBOX=true
PAYFAST_MERCHANT_ID=<own sandbox account>
PAYFAST_MERCHANT_KEY=<own sandbox account>
PAYFAST_PASSPHRASE=<own sandbox account>
API_DOCS_ENABLED=true                     # staging only
```

Vercel, Production: `VITE_DEMO_MODE=true`, `VITE_SITE_URL`, `VITE_API_BASE_URL`, `PRERENDER_API_URL`. Preview limited to branch `staging`: the same four plus `VITE_STAGING=true`. Both API URLs must point straight at Azure, not through a Vercel rewrite.

Never combine `prod` with the local `demo` profile; startup refuses it. Staging stays on the PayFast sandbox and the captured inbox even after production goes live. When an entity changes, set `JPA_DDL_AUTO=update` on staging first, check it starts, remove it, then do the same on production. A cold start takes about 4 minutes on B1.

## 2. Monitoring

The code is in (4 November 2026); everything is off until the settings below are filled in.

- [x] **Heartbeats built.** `Heartbeat` GETs a URL in the background (3 s timeout, logs failures, never throws, does nothing when the URL is blank). Pinged at the end of `ReservationReleaseJob` (every 5 minutes) and of the daily 09:00 cron in `FlowTriggers`. A job that throws doesn't ping.
- [x] **Sentry built** (errors only, no personal data, replays or tracing).
  - Backend: `sentry-spring-boot-4-starter` 8.61.0, configured by `SENTRY_DSN`, `SENTRY_ENVIRONMENT` and `SENTRY_SEND_DEFAULT_PII` (default `false`). A blank DSN turns it off.
  - Frontend: `@sentry/vue` 11.8.0 in `src/monitoring.js`, loaded as a separate chunk only when `VITE_SENTRY_DSN` is set (builds without it ship no Sentry code). The DSN's host is added to `connect-src` in the `<meta>` policy; the `vercel.json` header allows Sentry's ingest hosts.
- [ ] **Better Stack** (free): three uptime monitors at 3-minute intervals
  - Storefront: `https://guilt-free-cookie.vercel.app/`
  - API: `https://guiltfreecookie-api.azurewebsites.net/guiltFreeCookie/actuator/health`
  - Dependencies: `…/guiltFreeCookie/actuator/health/dependencies` (mail and outbox)
- [ ] Better Stack heartbeats: create two, "Reservations" (expected every 5 minutes, grace 10) and "Daily flows" (every 24 hours, grace 1 hour). Put their URLs in `HEARTBEAT_RESERVATIONS_URL` and `HEARTBEAT_DAILY_FLOWS_URL` on the production App Service.
- [ ] Optional status page, plus email and push alerts in the Better Stack app.
- [ ] **Sentry account** (free Developer plan): create a Spring Boot project and a Vue project.
  - Azure (production app): `SENTRY_DSN=<Spring Boot DSN>`, `SENTRY_ENVIRONMENT=production`. Staging too if wanted, with `SENTRY_ENVIRONMENT=staging`.
  - Vercel (Production; Preview for `staging` if wanted): `VITE_SENTRY_DSN=<Vue DSN>`, `VITE_SENTRY_ENVIRONMENT=production`, then redeploy.
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

- [x] Check the shipped mobile hero on a real iPhone / Safari: user reported smooth scrolling with nothing cut off (3 November 2026). Phones currently use the static hero.
- [x] Complete the standalone iPhone / Safari decoder check (4 November 2026, `safari-seek-check-v4`): all eight decoded-image checks passed, including rapid/reverse seeks, at 29–35ms. One compositor callback was absent despite matching pixels (`PIXELS_PASS`); this is decoder validation, not phone cinematic-scroll FPS certification. See [hero scroll validation](hero-scroll-validation.md).
