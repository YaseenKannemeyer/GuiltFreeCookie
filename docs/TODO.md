# What's left

The code is ready for hosting: `frontend/vercel.json`, `.github/workflows/deploy-api.yml`, and the health endpoints are all in place. Everything below is either account setup or work that hasn't been built yet.

Hosted target: **Vercel** for the storefront and **Azure for Students** for the API and MySQL. It costs $0 cash; the API uses about $13/month of student credit.

## Next: accounts to create (owner)

Monitoring is live (Better Stack and Sentry, 4 November 2026). Real email isn't set up yet. Each remaining service needs a sign-up in the owner's name; after that, Claude can do the rest of the setup with the key from each service.

**Keep keys out of chat and out of the repo.** Save each one in the Mac's Keychain (the command asks for the value), then tell Claude which ones are saved:

```
security add-generic-password -s "gfc sentry token" -a gfc -w
security add-generic-password -s "gfc brevo api key" -a gfc -w
security add-generic-password -s "gfc brevo smtp key" -a gfc -w
```

Type the command exactly as shown; the key goes only at the two prompts, never in the command. To replace a saved key, add `-U`. If a lookup still returns an old value, delete with `security delete-generic-password -s "<name>"` until it says "could not be found", then add it again. Check a key without printing it: `T=$(security find-generic-password -s "<name>" -w); echo "${#T} ${T:0:7}"` (a Sentry token is 71 characters starting `sntryu_`).

### 1. Better Stack: uptime monitors and heartbeats (about 5 minutes)

- [x] Sign up (free plan) at [betterstack.com/uptime](https://betterstack.com/uptime).
- [x] Monitors and heartbeats created by hand, so no API token is needed. Heartbeat URLs are set on the production App Service.
- [x] Dependencies monitor added (6 November 2026).
- [x] Storefront monitor points at `https://guilt-free-cookie.vercel.app/` (checked 6 November 2026).
- [x] **Reservations** heartbeat is green (6 November 2026).
- [x] **Daily flows** heartbeat paused (6 November 2026). It will never ping while `showcase` is on, because that profile turns the 09:00 job off (`app.flows.daily-cron=-`). Unpause it when `showcase` is removed (section 5).
- Push alerts, phone/SMS alerts and the phone app need the paid plan; the free plan alerts by email to `yaseenkannemeyer@gmail.com`.

### 2. Sentry: error reports (about 5 minutes)

- [x] Sign up (free **Developer** plan) at [sentry.io/signup](https://sentry.io/signup/). For POPIA, choose the **EU (Frankfurt)** data region if it's offered.
- [x] Create an **auth token** at [Settings → Account → Personal Tokens](https://sentry.io/settings/account/api/auth-tokens/) with Organization: Read, Project: Read & Write, Team: Read & Write.
- [x] Save it as `gfc sentry token`.
- [x] Projects created, settings in Azure and Vercel, production redeployed (details in section 2 below).

### 3. Domain: free through the GitHub Student Developer Pack (about 10 minutes)

- [ ] Open the [GitHub Student Developer Pack](https://education.github.com/pack) (signed in as `YaseenKannemeyer`) and claim a free domain. Two options are in the pack:
  - **Namecheap**: a free `.me` for one year at [nc.me](https://nc.me).
  - **.TECH**: a free `.tech` at [get.tech/github-student-developer-pack](https://get.tech/github-student-developer-pack).
- [ ] Note the renewal price before choosing; the free year doesn't renew for free.
- [ ] In the registrar's dashboard, set the domain's **nameservers** to Vercel's: `ns1.vercel-dns.com` and `ns2.vercel-dns.com`.
- [ ] Tell Claude the domain name. Claude adds it to the Vercel project (`vercel domains add`) and manages its DNS records from then on. Nameserver changes can take a few hours to take effect.

### 4. Brevo: sending real email (about 10 minutes, after the domain)

- [ ] Sign up (free plan, 300 emails a day) at [brevo.com](https://www.brevo.com/).
- [ ] Create an **API key** at [Settings → SMTP & API → API keys](https://app.brevo.com/settings/keys/api) and save it as `gfc brevo api key`.
- [ ] Create an **SMTP key** at [Settings → SMTP & API → SMTP](https://app.brevo.com/settings/keys/smtp) and save it as `gfc brevo smtp key`. Note the **SMTP login** shown on that page (it looks like `xxxx@smtp-brevo.com`) and tell Claude.
- [ ] Turn off **open and click tracking** in Brevo's transactional email settings: the app tracks these itself, and Brevo's link rewriting would wrap the unsubscribe links.
- Claude then adds and verifies the domain in Brevo, adds the SPF, DKIM and DMARC records in Vercel DNS, creates the sender `hello@<domain>`, sets the mail settings in Azure (section 3), and runs the mail-tester check.

### 5. ShipLogic sandbox credentials (apply now; they can take a few days)

- [x] Registered at [sandbox.shiplogic.com/register](https://sandbox.shiplogic.com/register) (6 November 2026).
- [x] API key `guiltfreecookie-sandbox` saved as `gfc shiplogic sandbox key` and working (6 November 2026). Base URL `https://api.shiplogic.com` (same host as live; the key picks the environment), `Authorization: Bearer <key>`. Test quote Cape Town 8001 → Johannesburg 2193, one 25×20×10 cm 1 kg parcel: only **ECO Economy, R95** (incl. VAT, flat rate, charged as 2 kg), 2 working days. No locker fields in the response. The API is documented at [shiplogic.com/api-docs](https://www.shiplogic.com/api-docs). Needed for section 4.
- If self-registration fails, ask `support@shiplogic.com` or The Courier Guy for sandbox credentials. Sandbox accounts may report "insufficient funds" when booking even though quotes work.
- [ ] Put the key on the **staging** App Service as `SHIPLOGIC_API_KEY` (Claude can do this from the Keychain entry). Keep production on the flat rate until the box sizes are real.

### 6. Google Places: address search at checkout (about 10 minutes)

- [x] Project `guiltfreecookie` created and billing added (6 November 2026). It's on the **free trial** ($300 credit) until **5 February 2027**. Before then, click **Upgrade to a full account** in Billing, then set the daily quotas (see the key item below), or Google stops the key and the address search disappears (customers can still type their address). Within Google's free monthly allowance the bill stays at $0.
- [x] **Places API (New)** enabled (6 November 2026).
- [x] API key created and tested (a real search works). Saved as `gfc google places key`.
- [x] Restricted (6 November 2026, checked: Geocoding is refused, Places works): application restrictions **None** (the server's 43 possible Azure outbound IPs make an IP list impractical), **API restrictions** → Places API (New) only. Quotas can't be changed on the free trial (and the card can't be charged during it). When upgrading to a full account, set **AutocompletePlacesRequest per day** and **GetPlaceRequest per day** to 1,000 at [Places API (New) quotas](https://console.cloud.google.com/google/maps-apis/quotas?project=guiltfreecookie) so even a leaked key stays within the free allowance. The key was pasted in chat once: consider **Rotate key** later.
- [ ] Under **Billing → Budgets & alerts**, add a budget of $5 with email alerts, so a surprise bill can't happen quietly.
- [x] `GOOGLE_PLACES_API_KEY` and `SHIPLOGIC_API_KEY` set on the **staging** App Service (6 November 2026). They take effect once this code is deployed to staging. Production after checking staging.

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

Never combine `prod` with the local `demo` profile; startup refuses it. Staging stays on the PayFast sandbox and the captured inbox even after production goes live. When an entity changes, set `JPA_DDL_AUTO=update` on staging first, check it starts, remove it, then do the same on production. A cold start takes about 4 minutes on B1. A Vercel build fails if the API is still starting (the prerender step gives the catalogue 10 seconds), for example right after an Azure settings change; wait until `/actuator/health` is `UP` and redeploy with `npx vercel redeploy https://guilt-free-cookie.vercel.app --environment production`.

## 2. Monitoring

The code is in and switched on (4 November 2026). Alerts from both services go by email.

- [x] **Heartbeats built.** `Heartbeat` GETs a URL in the background (3 s timeout, logs failures, never throws, does nothing when the URL is blank). Pinged at the end of `ReservationReleaseJob` (every 5 minutes) and of the daily 09:00 cron in `FlowTriggers`. A job that throws doesn't ping.
- [x] **Sentry built** (errors only, no personal data, replays or tracing).
  - Backend: `sentry-spring-boot-4-starter` 8.61.0, configured by `SENTRY_DSN`, `SENTRY_ENVIRONMENT` and `SENTRY_SEND_DEFAULT_PII` (default `false`). A blank DSN turns it off.
  - Frontend: `@sentry/vue` 11.8.0 in `src/monitoring.js`, loaded as a separate chunk only when `VITE_SENTRY_DSN` is set (builds without it ship no Sentry code). The DSN's host is added to `connect-src` in the `<meta>` policy; the `vercel.json` header allows Sentry's ingest hosts.
- [x] **Better Stack** (free): three uptime monitors at 3-minute intervals, default timeout, 3-minute recovery period, 5-minute confirmation period (longer than a cold start), regions Europe and North America
  - [x] Storefront: `https://guilt-free-cookie.vercel.app/` (made during onboarding; URL checked 6 November 2026)
  - [x] API: `https://guiltfreecookie-api.azurewebsites.net/guiltFreeCookie/actuator/health`
  - [x] Dependencies: `…/guiltFreeCookie/actuator/health/dependencies` (mail and outbox), created 6 November 2026
- [x] Better Stack heartbeats (2026-11-04, URLs set on production): create two, "Reservations" (expected every 5 minutes, grace 10) and "Daily flows" (every 24 hours, grace 1 hour). Put their URLs in `HEARTBEAT_RESERVATIONS_URL` and `HEARTBEAT_DAILY_FLOWS_URL` on the production App Service.
- [ ] Optional: a public status page (one is included in the free plan).
- [ ] Optional: **Grafana Cloud** dashboards (free tier). The API would push Micrometer metrics over OTLP, with no agent: requests, errors, response times, JVM, database pool, plus shop numbers (orders, revenue, address searches, courier fallbacks and bookings). Needs a grafana.com sign-up and an OTLP token saved in Keychain.
- [x] **Sentry account** (free Developer plan, EU region, org `guilt-free-cookie`): projects `guiltfreecookie-api` and `guiltfreecookie-web` created 2026-11-04. DSNs set on both Azure apps and in Vercel (Production and Preview `staging`). Vercel production redeployed with it.
  - Azure: `SENTRY_DSN` (API project DSN) on both apps; `SENTRY_ENVIRONMENT=production` / `staging`.
  - Vercel: `VITE_SENTRY_DSN` (web project DSN, added as public `config` type; a browser DSN is meant to be public) and `VITE_SENTRY_ENVIRONMENT`, for Production and Preview `staging`. Staging picks them up on its next push.
  - Each project has Sentry's default alert rule (email on new issues). PII stays off (POPIA).

## 3. Real email (only needed when leaving the captured demo inbox)

- [ ] Get a domain (free through the GitHub Student Developer Pack) and point it at Vercel.
- [ ] **Brevo** (free, 300 emails/day): verify the domain (SPF, DKIM, DMARC), add sender `hello@<domain>`, generate an SMTP key, and turn off Brevo's open and click tracking (the app tracks these itself, and Brevo's link rewriting would wrap the unsubscribe links).
- [ ] Settings: `MAIL_HOST=smtp-relay.brevo.com`, `MAIL_PORT=587`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH=true`, `MAIL_STARTTLS=true`, `MAIL_FROM`, `SITE_SUPPORT_EMAIL`, `FLOWS_ENABLED=false` to start with.
- [ ] Check with [mail-tester.com](https://www.mail-tester.com) (aim for 9/10 or better) and confirm Gmail's **Show original** shows SPF, DKIM and DMARC all as `PASS`.

## 4. Live courier rates and address search: ShipLogic (The Courier Guy) and Google Places

Stage 1 is built (6 November 2026, not yet deployed). Stage 2, booking and tracking, isn't.
- [x] Sandbox credentials (see "Next" section 5).
- [x] **Live courier quotes.** `ShipLogicClient` + `LiveShippingProvider` (wraps the flat-rate `RuleBasedShippingProvider`). Quotes by delivery area only (suburb, city, province, postal code; the street never goes to ShipLogic), from the service levels in `SHIPLOGIC_SERVICE_LEVELS` (default `ECO`; the "Flyer" services need a courier satchel, not a box), cheapest wins. Falls back to the flat rate on any error, an unknown area or after 3 s. Cached per area and cookie count for 10 minutes. Without an area the courier price is the flat rate marked `estimate`. The price is fixed on the order at checkout. Off while `SHIPLOGIC_API_KEY` is blank.
- [x] **Box size and weight** (`app.shiplogic.box.*`): made-up demo figures for the Box of 24 with packing, 32×24×10 cm, 280 g empty, 45 g a cookie. Replace with measured figures before it's a real shop.
- [x] **Collection address**: made-up `27 Spine Road, Strandfontein, Cape Town 7798` (`SHIPLOGIC_COLLECTION_*`). ShipLogic accepts it. Replace with the real bakery address before it's a real shop.
- [x] **Address search at checkout.** A "Search your address" box under Delivery address suggests South African addresses as you type (150 ms after the last keystroke, no Enter needed; arrow keys and Enter pick one) and fills in number, street, unit/complex, suburb, city, province and postal code. If Google has no street number it asks for one. The courier price then updates for that area. `AddressSearchService` calls Google Places (New) from the server, so the key never reaches the browser; 60 requests a minute per client. Hidden while `GOOGLE_PLACES_API_KEY` is blank. Privacy policy section 6 now names Google.
- [x] Tests: `ShipLogicClientTest`, `LiveShippingProviderTest`, `AddressSearchServiceTest`; checked in a browser (desktop and phone) against the ShipLogic sandbox and a stand-in for Google.
- [x] **Book courier** (stage 2, 6 November 2026, not yet deployed). On a paid courier order, the admin page shows **Book courier**: it quotes again with the full addresses, books the cheapest allowed service (`POST /v2/shipments`, `customer_reference` = order number), then ships the order with the tracking number and link, so the customer gets the shipped email. ShipLogic's own reason for a refusal is shown to the admin. The manual tracking form stays under "Or enter tracking yourself". Only on `SHIPLOGIC_BOOKING_DAYS` (default Monday–Wednesday, shop time).
- [x] **Download waybill**: the courier's PDF label (`/v2/shipments/label`), opened in a new tab.
- [x] **Delivered automatically**: every 30 minutes, shipped Courier Guy orders are checked (`/v2/tracking/shipments`); "delivered" moves the order to Delivered, which starts the delivered email flow. A poll instead of a webhook: ShipLogic's webhook topics and payloads (`/v2/accounts/webhooks`) aren't documented anywhere we can read. Ask `support@shiplogic.com` for them if instant updates are wanted.
- [x] **`delivery` health check** in the `dependencies` group (Better Stack's dependencies monitor): down if ShipLogic doesn't answer or refuses the key. Up while the key is blank.
- [x] Tested against the sandbox from the admin page in a browser: booked, tracking saved, waybill PDF downloaded, then the test shipments were cancelled. Tests: `ShipLogicBookingTest`, `CourierBookingTest`.
- [ ] **Customer tracking link**: `SHIPLOGIC_TRACKING_URL`, default `https://portal.thecourierguy.co.za/track?ref={ref}`. Not confirmed: open one in a browser with a real tracking number. Staging uses `https://sandbox.shiplogic.com/track?ref={ref}`.
- [ ] **Courier contact email** (`SHIPLOGIC_COLLECTION_EMAIL`, defaults to the site support email): ShipLogic refuses an email whose domain doesn't exist, so staging and the showcase's `hello@example.test` need a real one before booking works there.
- [x] **PUDO lockers switched off** for new databases (`SHIPPING_PUDO_ENABLED`, default false). Existing databases: switch PUDO off in **Admin → Delivery**.
- Optional later: a real locker picker (ShipLogic lists Courier Guy lockers via `/v2/pickup-points` and books to one with `delivery_pickup_point_id`), and instant updates via webhooks.

## 4b. Pack sizes and prices (6 November 2026, not yet deployed)

Every cookie sells as **Single R5**, **Box of 6 R27** (R4.50 each, save 10%), **Box of 12 R48** (R4.00, save 20%) and **Box of 24 R84** (R3.50, save 30%). The product page shows each pack's price and a "Save x%" badge (`VariantView.savingPercent`, measured against the single).
- [x] New local demo databases get these packs (`DemoData`). An existing local demo database: `scripts/reprice.sh local`.
- [ ] After deploying, run `scripts/reprice.sh staging`, check the shop, then `scripts/reprice.sh production`. It asks for an admin email and password, updates the four packs on every visible cookie and adds a missing Box of 24. Safe to run twice.
- [x] **Minimum order R300** (cookies' subtotal before discounts and delivery; `ORDER_MINIMUM_SUBTOTAL`, 0 turns it off). The cart says how much more to add and blocks Continue; checkout sends a cart under it back to the cart, and the server refuses it.
- [ ] Decide the free-delivery threshold. It's R500, which is now about six Boxes of 24, against R80–R95 delivery.

## 5. Before it's ever a real shop

- [ ] The owner confirms the recipes, allergens and prices for all 12 cookies, including the chocolate suppliers' allergens and cross-contact. Don't invent nutrition values.
- [ ] Review the 11 generated cookie images (only Double Chocolate Chunk was approved individually).
- [ ] Get a legal check on the "Diabetic friendly" category name (R146: no implied disease claims).
- [ ] Registered business name, registration number and physical address (`SITE_ADDRESS`, required by ECTA s43), plus the rest of the `SITE_*` contact details.
- [ ] Review the privacy, terms, refund and cookie policies.
- [ ] PayFast **live** merchant ID, key and passphrase; set `PAYFAST_SANDBOX=false`.
- [ ] Remove the `showcase` profile and set `VITE_DEMO_MODE=false`. Then unpause the **Daily flows** heartbeat in Better Stack and check it turns green after the next 09:00 run.
- [ ] Real-card test order, then a refund recorded in admin and paid back in the PayFast dashboard. Test unsubscribing from a marketing email.

## 6. Smaller loose ends

- [x] Check the shipped mobile hero on a real iPhone / Safari: user reported smooth scrolling with nothing cut off (3 November 2026). Phones currently use the static hero.
- [x] Complete the standalone iPhone / Safari decoder check (4 November 2026, `safari-seek-check-v4`): all eight decoded-image checks passed, including rapid/reverse seeks, at 29–35ms. One compositor callback was absent despite matching pixels (`PIXELS_PASS`); this is decoder validation, not phone cinematic-scroll FPS certification. See [hero scroll validation](hero-scroll-validation.md).
