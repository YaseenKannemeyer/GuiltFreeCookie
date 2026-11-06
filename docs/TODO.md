# What's left

Storefront on **Vercel** (`guiltfreecookie.me`), API and MySQL on **Azure for Students**. It costs $0 cash; the API uses about $13/month of student credit. Both environments still run as a showcase (`prod,showcase`: PayFast and ShipLogic sandboxes, captured inbox).

## Open now (checked 6 November 2026)

In order. **(you)** = the owner, **(Claude)** = Claude can do it once the owner's part is done.

1. [x] **Production on `guiltfreecookie.me`** (6 November 2026): `FRONTEND_BASE_URL` and `CORS_ALLOWED_ORIGINS` set, Vercel production redeployed; canonical links, sitemap (20 pages) and robots.txt use the new domain, and the API accepts calls from it.
2. [x] **Production sends real email** through Brevo from `Guilt Free Cookie <hello@guiltfreecookie.me>` (6 November 2026; test order confirmation received). Settings in section 3. Staging keeps the demo inbox.
3. [x] **Better Stack** Storefront monitor now checks `https://guiltfreecookie.me/` (6 November 2026).
4. **(you) Email leftovers**: replace the Brevo API key (it was pasted in chat), delete ImprovMX's `new-alias`, (Brevo's tracking can't be turned off on the free plan: see "Next" section 4.)
5. **(you) Admin → Delivery**: production has PUDO off; **staging still offers PUDO**. Free delivery from R750 checked on staging (an R840 cart is free). On production it couldn't be checked because stock is too low to build a R750 cart.
6. **(you) Restock production before the presentation**: on 6 November 2026 Raspberry Coconut and Toasted Almond & Vanilla were out of stock and five more were low (Cinnamon Pecan couldn't fill a Box of 6). A Box of 24 uses 24 cookies, so 50 doesn't go far: run `scripts/restock.sh production` with a higher number.
7. **(you) Commit** (end of the 6 November session) the uncommitted changes: R750 default (`application.properties`), the R750 pre-fill (`AdminShippingView.vue`) and this file. The R750 default only reaches Azure with the next API deploy, and only matters for new databases.
8. **(you) Google card hold**: check the bank statement around **20 November 2026**. If the R847.81 hold is still there, ask Google (**Billing → Help → Contact billing support**) or the bank to release it. The $5 budget alert is done.


## Done: 6 November release

Live on staging and production (PR #46 `0438fc4`, then the responsive admin work in PR #47): address search (Google key on both apps, checked on production), courier quotes and booking (both apps, with the **sandbox** ShipLogic key; production checked: R95 quote to Johannesburg), new pack prices (`scripts/reprice.sh production` run, Vercel production redeployed). The staging 500s that morning were the API restarting, not a code fault.
- [x] **Prerender retry** (on `main` since PR #47): on Vercel it retries every 20 seconds for up to 10 minutes (`PRERENDER_WAIT_SECONDS`, 0 locally). Without it, every merge that also deploys the API fails the Vercel build: staging on 6 November at 06:53 UTC and production at 17:18 UTC both had to be redeployed by hand.
- [x] `WEBSITES_CONTAINER_START_TIME_LIMIT=600` on both App Services (6 November 2026; it was the default, and the probe gave up after 282 seconds that morning).
- [x] **Slow startup: left as it is** (decided 6 November 2026). The API takes 190–265 seconds to start on B1, and 487 seconds when both apps start at once (they share one core). The 600-second limit covers it. **Never restart or deploy staging and production at the same time**; wait for one to answer before touching the other. Memory is also tight (85–90% with two 512 MB heaps on the plan's 1.75 GB). Not the network share: a startup command running the jar from `/tmp` still took over 240 seconds and then exited with code 1, and its repeated restarts pinned the CPU at 100% until production went down too (6 November, 17:51–18:09 UTC). Fixed by stopping staging until production answered, then starting staging again. Don't retry that startup command. If it's ever needed: a Java 25 AOT cache built in CI, or a B2 plan. Right after a restart the first address search or two can fail while the JVM warms up.
- [ ] **Switch PUDO off on production** in **Admin → Delivery**: checkout still offers PUDO locker at R60 (checked 6 November 2026; only new databases default to off).

## Next: accounts to create (owner)

Monitoring is live (Better Stack and Sentry, 4 November 2026), the domain too (6 November 2026). Real email isn't set up yet. Each remaining service needs a sign-up in the owner's name; after that, Claude can do the rest of the setup with the key from each service.

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
- [ ] Storefront monitor: change from `https://guilt-free-cookie.vercel.app/` to `https://guiltfreecookie.me/`.
- [x] **Reservations** heartbeat is green (6 November 2026).
- [x] **Daily flows** heartbeat paused (6 November 2026). It will never ping while `showcase` is on, because that profile turns the 09:00 job off (`app.flows.daily-cron=-`). Unpause it when `showcase` is removed (section 5).
- Push alerts, phone/SMS alerts and the phone app need the paid plan; the free plan alerts by email to `yaseenkannemeyer@gmail.com`.

### 2. Sentry: error reports (about 5 minutes)

- [x] Sign up (free **Developer** plan) at [sentry.io/signup](https://sentry.io/signup/). For POPIA, choose the **EU (Frankfurt)** data region if it's offered.
- [x] Create an **auth token** at [Settings → Account → Personal Tokens](https://sentry.io/settings/account/api/auth-tokens/) with Organization: Read, Project: Read & Write, Team: Read & Write.
- [x] Save it as `gfc sentry token`.
- [x] Projects created, settings in Azure and Vercel, production redeployed (details in section 2 below).

### 3. Domain: `guiltfreecookie.me` (done 6 November 2026)

- [x] Claimed free for one year through the GitHub Student Developer Pack at Namecheap (registered 6 November 2026, **expires 6 November 2027**). From South Africa the free `.me` only works by entering through the pack page ([education.github.com/pack](https://education.github.com/pack) → Namecheap → connect GitHub) and choosing the **GitHub Pages** free option at checkout; the "student email" route is limited to US, UK, Canadian and Australian universities. Don't activate the GitHub Pages hosting it offers afterwards.
- [x] Domain privacy is on (WITHHELD FOR PRIVACY).
- [x] Nameservers set to `ns1.vercel-dns.com` and `ns2.vercel-dns.com`; live within minutes. Claude manages the DNS records in Vercel from now on.
- [x] Added to the Vercel project with `www.guiltfreecookie.me`, which redirects to `guiltfreecookie.me` (308). HTTPS by Vercel; the free Namecheap SSL certificate isn't needed.
- [x] `VITE_SITE_URL=https://guiltfreecookie.me` set on Vercel production (6 November 2026); takes effect at the next production redeploy ("Open now" 2).
- [ ] Before November 2027: decide whether to renew (Namecheap emails 30 days before; check the `.me` renewal price). If an SSL certificate was added to the order, switch its auto-renew off (Products → SSL Certificates).

### 4. Brevo: sending email (set up 6 November 2026)

- [x] Account `GuiltFreeCookie` on the free plan (300 emails a day) at [brevo.com](https://www.brevo.com/).
- [x] **API key** saved as `gfc brevo api key` (89 characters, `xkeysib-`). Only Claude uses it, for setup; the shop sends over SMTP. Brevo blocks API calls from IP addresses it hasn't seen: the home IP was authorised under **Security → Authorized IPs**. Keep **IP blocking deactivated for SMTP keys**, or Azure's changing outbound IPs get refused.
- [ ] **Replace the API key**: the current one was pasted in chat on 6 November 2026 (and backs the MCP key). Generate a new one with **Restricted** access (Transactional email: Read, Account: Read/write), save it with `security add-generic-password -U -s "gfc brevo api key" -a gfc -w "$(pbpaste)"; pbcopy < /dev/null`, then deactivate the old one and the MCP key.
- [x] **SMTP key** saved as `gfc brevo smtp key` (90 characters, `xsmtpsib-`). **SMTP login `c1dacb001@smtp-brevo.com`**, host `smtp-relay.brevo.com`, port 587 with STARTTLS.
- [x] Open and click tracking: checked 6 November 2026, it can't be switched off on the free plan (see the mail-tester item below). Optional: **Settings → Automations → Transactional emails → Tracking → Anonymous email tracking: Yes** keeps it from being tied to a person.
- [x] **Domain `guiltfreecookie.me` authenticated** in Brevo through its API. DNS records in Vercel: DKIM CNAMEs `brevo1._domainkey` → `b1.guiltfreecookie-me.dkim.brevo.com` and `brevo2._domainkey` → `b2.guiltfreecookie-me.dkim.brevo.com`, TXT `brevo-code:51681139bf534fb305c56b99c9ef5668`, DMARC `_dmarc` `v=DMARC1; p=none; rua=mailto:rua@dmarc.brevo.com`. Move DMARC to `p=quarantine` once a few weeks of reports look clean.
- [x] **Sender `hello@guiltfreecookie.me`** created and active (no SPF or DKIM errors).
- [x] Test sent over SMTP from `hello@guiltfreecookie.me` to the owner's Gmail (Brevo accepted it).
- [x] Gmail (**⋮ → Show original**, 6 November 2026): **SPF `PASS`, DKIM `PASS` with domain guiltfreecookie.me, DMARC `PASS`**, delivered in 4 seconds.
- [x] [mail-tester.com](https://www.mail-tester.com) score **8.6/10** (6 November 2026): SPF, DKIM and DMARC pass, not on any blocklist, good IP reputation. Deductions: Brevo's tracking pixel (no alt text, little text in a short test email) and the bounce address being Brevo's domain (`sender-sib.com`). Real order emails are longer, so they score a bit higher.
- [ ] **Brevo tracks every email and there's no switch for it**: over SMTP it rewrites links to `…sendibt3.com/tr/cl/…`, adds an open-tracking pixel, and adds its own `List-Unsubscribe` header even to order emails. On the free plan this can't be turned off; Brevo staff say it can be disabled **on request** ([community thread](https://community.brevo.com/t/no-way-to-disable-by-option-tracking-in-transactional-e-mail/201)). Links still work (they redirect). Before it's a real shop, ask Brevo support to disable tracking for the account, or move sending to a provider that allows it. Risk meanwhile: a customer pressing "unsubscribe" on an order email can get blocked from later order emails (check Brevo → Contacts → Blocklisted if an order email doesn't arrive). Mention Brevo's tracking in the privacy policy (POPIA).
- Vercel DNS changes are run by the owner (`npx vercel dns add …` from `frontend/`); Claude's are blocked by permissions.

### 4b. ImprovMX: receiving email at `hello@guiltfreecookie.me` (set up 6 November 2026)

Brevo only sends, so without this, replies to `hello@` were lost.
- [x] Free [ImprovMX](https://improvmx.com) account (signed up as `yaseenkannemeyer@gmail.com`), domain `guiltfreecookie.me` added.
- [x] Alias **`hello`** → `yaseenkannemeyer@gmail.com` (the alias box takes only the part before the `@`). Delete the unused `new-alias`. A catch-all `*` alias can be added back so any other address also arrives.
- [x] DNS in Vercel: MX `mx1.improvmx.com` (priority 10) and `mx2.improvmx.com` (20). **SPF** replaced with one record covering both: `v=spf1 include:spf.brevo.com include:spf.improvmx.com ~all` (a domain may only have one SPF record).
- [x] Test sent to `hello@guiltfreecookie.me` through Brevo.
- [x] Forwarding works: ImprovMX received and forwarded the test (6 November 2026). A test from your own Gmail to `hello@` lands in Spam or shows "via improvmx.net" (Gmail's loop protection); test from a different address instead. Real customer mail isn't affected.
- [ ] Refresh the ImprovMX dashboard to confirm it shows the domain as configured (its "not active" email was sent before the MX records went in).
- The free plan forwards only; sending stays with Brevo. Its premium plan isn't needed.

### 5. ShipLogic sandbox credentials (apply now; they can take a few days)

- [x] Registered at [sandbox.shiplogic.com/register](https://sandbox.shiplogic.com/register) (6 November 2026).
- [x] API key `guiltfreecookie-sandbox` saved as `gfc shiplogic sandbox key` and working (6 November 2026). Base URL `https://api.shiplogic.com` (same host as live; the key picks the environment), `Authorization: Bearer <key>`. Test quote Cape Town 8001 → Johannesburg 2193, one 25×20×10 cm 1 kg parcel: only **ECO Economy, R95** (incl. VAT, flat rate, charged as 2 kg), 2 working days. No locker fields in the response. The API is documented at [shiplogic.com/api-docs](https://www.shiplogic.com/api-docs). Needed for section 4.
- The sandbox account (`GUI001`) is postpaid with a large credit limit, so a negative balance doesn't block bookings.
- [x] Sandbox key on **both** App Services as `SHIPLOGIC_API_KEY` (6 November 2026). A live key replaces it before it's a real shop (section 5).

### 6. Google Places: address search at checkout (about 10 minutes)

- [x] Project `guiltfreecookie` created and billing added (6 November 2026). It's on the **free trial** ($300 credit) until **5 February 2027**. Before then, click **Upgrade to a full account** in Billing, then set the daily quotas (see the key item below), or Google stops the key and the address search disappears (customers can still type their address). Within Google's free monthly allowance the bill stays at $0.
- [ ] **Google held R847.81 on the card** when billing was added (6 November 2026). The account shows $0 cost and is on the free trial, so it should be a card check that gets released. If it's still on the bank statement by about 20 November, ask Google (**Billing → Help → Contact billing support**) or the bank to release it.
- [x] **Places API (New)** enabled (6 November 2026).
- [x] API key created and tested (a real search works). Saved as `gfc google places key`.
- [x] Restricted (6 November 2026, checked: Geocoding is refused, Places works): application restrictions **None** (the server's 43 possible Azure outbound IPs make an IP list impractical), **API restrictions** → Places API (New) only. Quotas can't be changed on the free trial (and the card can't be charged during it). When upgrading to a full account, set **AutocompletePlacesRequest per day** and **GetPlaceRequest per day** to 1,000 at [Places API (New) quotas](https://console.cloud.google.com/google/maps-apis/quotas?project=guiltfreecookie) so even a leaked key stays within the free allowance. The key was pasted in chat once: consider **Rotate key** later.
- [x] $5 budget with email alerts under **Billing → Budgets & alerts** (6 November 2026).
- [x] `GOOGLE_PLACES_API_KEY` and `SHIPLOGIC_API_KEY` set on **staging**; `GOOGLE_PLACES_API_KEY` on **production** too (6 November 2026, address search checked). Production has the sandbox ShipLogic key too (6 November 2026).

## 1. Hosting

Set up on 3 November 2026; domain added 6 November 2026. Storefront on **Vercel**, API and MySQL on **Azure for Students** (South Africa North). Both environments run `prod,showcase`.

| | Staging | Production |
|---|---|---|
| Storefront | `guilt-free-cooki-git-7d1aa2-mogamat-yaseen-kannemeyers-projects.vercel.app` (Vercel shortened the branch URL; it stays fixed) | `guiltfreecookie.me` (`www.` redirects; `guilt-free-cookie.vercel.app` still works) |
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
- Won't do: rotating the `gfc_staging` database password (decided 6 November 2026; everything is shut down after the presentation, see section 7).
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
CORS_ALLOWED_ORIGINS=https://guiltfreecookie.me,https://www.guiltfreecookie.me,https://guilt-free-cookie.vercel.app   # staging: the staging storefront URL
MAIL_*, APP_MAIL_ENABLED, MANAGEMENT_HEALTH_MAIL_ENABLED, APP_SITE_SUPPORTEMAIL   # production only, see section 3
FRONTEND_BASE_URL=https://guiltfreecookie.me                   # staging: the staging storefront URL
PUBLIC_BASE_URL=https://guiltfreecookie-api.azurewebsites.net/guiltFreeCookie
UPLOAD_DIR=/home/uploads
JAVA_OPTS=-Xmx512m
DEMO_SIMULATED_PAYMENTS=false
PAYFAST_SANDBOX=true
PAYFAST_MERCHANT_ID=<own sandbox account>
PAYFAST_MERCHANT_KEY=<own sandbox account>
PAYFAST_PASSPHRASE=<own sandbox account>
API_DOCS_ENABLED=true                     # staging only
WEBSITES_CONTAINER_START_TIME_LIMIT=600
GOOGLE_PLACES_API_KEY=<gfc google places key>
SHIPLOGIC_API_KEY=<gfc shiplogic sandbox key>
SHIPLOGIC_TRACKING_URL=https://sandbox.shiplogic.com/track?ref={ref}
SHIPLOGIC_COLLECTION_EMAIL=hello@guiltfreecookie.me   # staging: yaseenkannemeyer@gmail.com
SHIPLOGIC_BOOKING_DAYS=MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY
SENTRY_DSN=<API project DSN>
SENTRY_ENVIRONMENT=production             # staging: staging
HEARTBEAT_RESERVATIONS_URL, HEARTBEAT_DAILY_FLOWS_URL   # production only
```

Vercel, Production: `VITE_DEMO_MODE=true`, `VITE_SITE_URL=https://guiltfreecookie.me`, `VITE_API_BASE_URL`, `PRERENDER_API_URL`, `VITE_SENTRY_DSN`, `VITE_SENTRY_ENVIRONMENT`. Preview limited to branch `staging`: the same four plus `VITE_STAGING=true`. Both API URLs must point straight at Azure, not through a Vercel rewrite.

Never combine `prod` with the local `demo` profile; startup refuses it. Staging stays on the PayFast sandbox and the captured inbox even after production goes live. When an entity changes, set `JPA_DDL_AUTO=update` on staging first, check it starts, remove it, then do the same on production. A cold start takes about 4 minutes on B1 (7 when both apps start together, which Azure's own maintenance also does). Any Azure settings change restarts that app. **Restart, deploy or change one app at a time.** On Vercel the prerender waits up to 10 minutes for the API; if a build still fails, wait until `/actuator/health` is `UP` and redeploy that deployment with `npx vercel redeploy <deployment url>`.

## 2. Monitoring

The code is in and switched on (4 November 2026). Alerts from both services go by email.

- [x] **Heartbeats built.** `Heartbeat` GETs a URL in the background (3 s timeout, logs failures, never throws, does nothing when the URL is blank). Pinged at the end of `ReservationReleaseJob` (every 5 minutes) and of the daily 09:00 cron in `FlowTriggers`. A job that throws doesn't ping.
- [x] **Sentry built** (errors only, no personal data, replays or tracing).
  - Backend: `sentry-spring-boot-4-starter` 8.61.0, configured by `SENTRY_DSN`, `SENTRY_ENVIRONMENT` and `SENTRY_SEND_DEFAULT_PII` (default `false`). A blank DSN turns it off.
  - Frontend: `@sentry/vue` 11.8.0 in `src/monitoring.js`, loaded as a separate chunk only when `VITE_SENTRY_DSN` is set (builds without it ship no Sentry code). The DSN's host is added to `connect-src` in the `<meta>` policy; the `vercel.json` header allows Sentry's ingest hosts.
- [x] **Better Stack** (free): three uptime monitors at 3-minute intervals, default timeout, 3-minute recovery period, 5-minute confirmation period (longer than a cold start), regions Europe and North America
  - [ ] Storefront: `https://guilt-free-cookie.vercel.app/` (made during onboarding); change to `https://guiltfreecookie.me/` ("Open now" 3)
  - [x] API: `https://guiltfreecookie-api.azurewebsites.net/guiltFreeCookie/actuator/health`
  - [x] Dependencies: `…/guiltFreeCookie/actuator/health/dependencies` (mail and outbox), created 6 November 2026
- [x] Better Stack heartbeats (2026-11-04, URLs set on production): create two, "Reservations" (expected every 5 minutes, grace 10) and "Daily flows" (every 24 hours, grace 1 hour). Put their URLs in `HEARTBEAT_RESERVATIONS_URL` and `HEARTBEAT_DAILY_FLOWS_URL` on the production App Service.
- [ ] **Public status page** (Better Stack, free; started 6 November 2026 for the presentation): `status.guiltfreecookie.me`, built from the three monitors. The owner creates it in Better Stack and adds the CNAME it shows with `npx vercel dns add guiltfreecookie.me status CNAME <target>`.
- [ ] **Grafana Cloud dashboards** (free tier; code built 6 November 2026, not yet deployed).
  - [x] Code: `micrometer-registry-otlp` plus `spring-boot-opentelemetry` (Spring Boot 4 only switches the OTLP exporter on when that module is present). Every minute the API pushes request counts and times, errors, JVM memory, the database pool, and shop numbers from `ShopMetrics`: `gfc.orders.placed`, `gfc.orders.paid`, `gfc.revenue`, `gfc.address.searches`, `gfc.courier.quotes` (live or fallback), `gfc.courier.bookings`, `gfc.emails` (sent, failed, expired). Tags `application=guiltfreecookie-api` and `environment` (from `SENTRY_ENVIRONMENT`). Off unless `OTLP_METRICS_ENABLED=true`. Checked locally against a stand-in receiver (batches arrive with the `Authorization` header); `ShopMetricsTest`; all 490 tests pass.
  - [x] Grafana Cloud account (free tier; a 14-day trial of extra features ends 20 November 2026 and drops back to free; no card added). Org and stack `broadtarantula971` ([broadtarantula971.grafana.net](https://broadtarantula971.grafana.net)), region EU West. Token `1910869-guiltfreecookie-api` from **Cloud Portal → stack Details → OpenTelemetry → Configure**, saved as `gfc grafana otlp auth` (the whole `Basic …` value, 246 characters; it was pasted in chat). Endpoint `https://otlp-gateway-prod-eu-west-6.grafana.net/otlp`.
  - [x] Checked 7 November 2026: the token is accepted (200, 401 without it), and a local run sent metrics to Grafana Cloud with no failures (tagged `environment=local`). In Grafana: **Explore → Prometheus → `jvm_memory_used_bytes`**.
  - [ ] Settings on staging, then production: `OTLP_METRICS_ENABLED=true`, `OTLP_METRICS_URL=https://otlp-gateway-prod-eu-west-6.grafana.net/otlp/v1/metrics`, `OTLP_METRICS_AUTHORIZATION=<gfc grafana otlp auth>`. Then deploy (commit, push to `staging`, merge to `main`, run the production deploy), one app at a time.
  - [ ] Claude: a dashboard to import, once the metric names show up in Grafana.
- [x] **Sentry account** (free Developer plan, EU region, org `guilt-free-cookie`): projects `guiltfreecookie-api` and `guiltfreecookie-web` created 2026-11-04. DSNs set on both Azure apps and in Vercel (Production and Preview `staging`). Vercel production redeployed with it.
  - Azure: `SENTRY_DSN` (API project DSN) on both apps; `SENTRY_ENVIRONMENT=production` / `staging`.
  - Vercel: `VITE_SENTRY_DSN` (web project DSN, added as public `config` type; a browser DSN is meant to be public) and `VITE_SENTRY_ENVIRONMENT`, for Production and Preview `staging`. Staging picks them up on its next push.
  - Each project has Sentry's default alert rule (email on new issues). PII stays off (POPIA).

## 3. Real email (only needed when leaving the captured demo inbox)

- [x] Domain `guiltfreecookie.me`, on Vercel DNS (6 November 2026).
- [x] **Brevo** account, keys, domain authenticated and sender `hello@guiltfreecookie.me` (6 November 2026, "Next" section 4).
- [x] Replies to `hello@guiltfreecookie.me` forwarded to Gmail by ImprovMX ("Next" section 4b).
- [x] **Production sends real email** (6 November 2026, owner's decision), still under `showcase`. Settings: `MAIL_HOST=smtp-relay.brevo.com`, `MAIL_PORT=587`, `MAIL_USERNAME=c1dacb001@smtp-brevo.com`, `MAIL_PASSWORD=<gfc brevo smtp key>`, `MAIL_SMTP_AUTH=true`, `MAIL_STARTTLS=true`, `MAIL_FROM=Guilt Free Cookie <hello@guiltfreecookie.me>`. The showcase profile pins three values, so they're overridden with the property's own environment name (environment variables beat profile files): `APP_MAIL_ENABLED=true`, `MANAGEMENT_HEALTH_MAIL_ENABLED=true` (the `mail` check in Better Stack's dependencies monitor), `APP_SITE_SUPPORTEMAIL=hello@guiltfreecookie.me`. Daily marketing flows stay off (`showcase` sets `app.flows.enabled=false`). Checked: health and dependencies `UP`, a test order's confirmation arrived.
- [x] `SHIPLOGIC_COLLECTION_EMAIL=hello@guiltfreecookie.me` on production. Staging still uses the owner's Gmail.
- [ ] When `showcase` is removed (section 5), the `APP_*` and `MANAGEMENT_*` overrides above can go: `prod` already enables mail.
- [ ] Check with [mail-tester.com](https://www.mail-tester.com) (aim for 9/10 or better) and confirm Gmail's **Show original** shows SPF, DKIM and DMARC all as `PASS`.

## 4. Live courier rates and address search: ShipLogic (The Courier Guy) and Google Places

Stages 1 and 2 are live on staging and production (6 November 2026), both on the ShipLogic **sandbox** key, like PayFast's sandbox: bookings are test shipments. Before it's a real shop: a live ShipLogic key, the measured box sizes, the real collection address, and `SHIPLOGIC_TRACKING_URL` / `SHIPLOGIC_BOOKING_DAYS` back to their defaults (production uses the sandbox tracking page and every day for demos).
- [x] Sandbox credentials (see "Next" section 5).
- [x] **Live courier quotes.** `ShipLogicClient` + `LiveShippingProvider` (wraps the flat-rate `RuleBasedShippingProvider`). Quotes by delivery area only (suburb, city, province, postal code; the street never goes to ShipLogic), from the service levels in `SHIPLOGIC_SERVICE_LEVELS` (default `ECO`; the "Flyer" services need a courier satchel, not a box), cheapest wins. Falls back to the flat rate on any error, an unknown area or after 3 s. Cached per area and cookie count for 10 minutes. Without an area the courier price is the flat rate marked `estimate`. The price is fixed on the order at checkout. Off while `SHIPLOGIC_API_KEY` is blank.
- [x] **Box size and weight** (`app.shiplogic.box.*`): made-up demo figures for the Box of 24 with packing, 32×24×10 cm, 280 g empty, 45 g a cookie. Replace with measured figures before it's a real shop.
- [x] **Collection address**: made-up `27 Spine Road, Strandfontein, Cape Town 7798` (`SHIPLOGIC_COLLECTION_*`). ShipLogic accepts it. Replace with the real bakery address before it's a real shop.
- [x] **Address search at checkout.** A "Search your address" box under Delivery address suggests South African addresses as you type (150 ms after the last keystroke, no Enter needed; arrow keys and Enter pick one) and fills in number, street, unit/complex, suburb, city, province and postal code. If Google has no street number it asks for one. The courier price then updates for that area. `AddressSearchService` calls Google Places (New) from the server, so the key never reaches the browser; 60 requests a minute per client. Hidden while `GOOGLE_PLACES_API_KEY` is blank. Privacy policy section 6 now names Google.
- [x] Tests: `ShipLogicClientTest`, `LiveShippingProviderTest`, `AddressSearchServiceTest`; checked in a browser (desktop and phone) against the ShipLogic sandbox and a stand-in for Google.
- [x] **Book courier** (stage 2, 6 November 2026). On a paid courier order, the admin page shows **Book courier**: it quotes again with the full addresses, books the cheapest allowed service (`POST /v2/shipments`, `customer_reference` = order number), then ships the order with the tracking number and link, so the customer gets the shipped email. ShipLogic's own reason for a refusal is shown to the admin. The manual tracking form stays under "Or enter tracking yourself". Only on `SHIPLOGIC_BOOKING_DAYS` (default Monday–Wednesday, shop time).
- [x] **Download waybill**: the courier's PDF label (`/v2/shipments/label`), opened in a new tab.
- [x] **Delivered automatically**: every 30 minutes, shipped Courier Guy orders are checked (`/v2/tracking/shipments`); "delivered" moves the order to Delivered, which starts the delivered email flow. A poll instead of a webhook: ShipLogic's webhook topics and payloads (`/v2/accounts/webhooks`) aren't documented anywhere we can read. Ask `support@shiplogic.com` for them if instant updates are wanted.
- [x] **`delivery` health check** in the `dependencies` group (Better Stack's dependencies monitor): down if ShipLogic doesn't answer or refuses the key. Up while the key is blank.
- [x] Tested against the sandbox from the admin page in a browser: booked, tracking saved, waybill PDF downloaded, then the test shipments were cancelled. Tests: `ShipLogicBookingTest`, `CourierBookingTest`.
- [x] **Booked from production** (order `GFC-261106-JGXY`, sandbox tracking `RDBF7N`, R95), then both open test shipments were cancelled through the API (`POST /v2/shipments/cancel`, 6 November 2026). The sandbox account is postpaid, so a negative balance doesn't block bookings.
- [x] **ShipLogic tracking privacy** on the sandbox account (6 November 2026; portal → Account → Account settings → General → Tracking information): switched on **Hide collection details, the delivery contact and the service level** and **Automatically remove tracking information 7 days after delivery** (POPIA). **Do the same on the live account** when there is one; settings don't carry over.
- [ ] **Customer tracking link**: `SHIPLOGIC_TRACKING_URL`, default `https://portal.thecourierguy.co.za/track?ref={ref}`, for the live account; confirm it with the first real tracking number. Both apps use `https://sandbox.shiplogic.com/track?ref={ref}` for now.
- [ ] Optional: upload the shop logo in ShipLogic (Account settings → Logo) so it prints on waybills, e.g. `frontend/public/icon-512.png`.
- [x] **Courier contact email** (`SHIPLOGIC_COLLECTION_EMAIL`, defaults to the site support email; ShipLogic refuses an email whose domain doesn't exist, such as the showcase's `hello@example.test`). Both apps use `yaseenkannemeyer@gmail.com` (6 November 2026). Switch to `hello@guiltfreecookie.me` once Brevo is set up.
- [x] **PUDO lockers switched off** for new databases (`SHIPPING_PUDO_ENABLED`, default false). Staging and production still offer it: switch it off in **Admin → Delivery** ("Open now" 5).
- Optional later: a real locker picker (ShipLogic lists Courier Guy lockers via `/v2/pickup-points` and books to one with `delivery_pickup_point_id`), and instant updates via webhooks.

## 4b. Pack sizes and prices (live 6 November 2026)

Every cookie sells as **Single R5**, **Box of 6 R27** (R4.50 each, save 10%), **Box of 12 R48** (R4.00, save 20%) and **Box of 24 R84** (R3.50, save 30%). The product page shows each pack's price and a "Save x%" badge (`VariantView.savingPercent`, measured against the single).
- [x] New local demo databases get these packs (`DemoData`). An existing local demo database: `scripts/reprice.sh local`.
- [x] Repriced on staging and production (6 November 2026). It asks for an admin email and password, updates the four packs on every visible cookie and adds a missing Box of 24. Safe to run twice.
- [x] **Minimum order R300** (cookies' subtotal before discounts and delivery; `ORDER_MINIMUM_SUBTOTAL`, 0 turns it off). The cart says how much more to add and blocks Continue; checkout sends a cart under it back to the cart, and the server refuses it.
- [x] **Free delivery from R750** (decided 6 November 2026; was R500). New databases get it (`SHIPPING_FREE_THRESHOLD`, default 750).
- [ ] Set **Free delivery from** to R750 in **Admin → Delivery** on staging and production (existing databases keep R500 until changed).

## 5. Before it's ever a real shop

Reviewed 6 November 2026. Grouped by who has to act; roughly in order within each group.

**Business and legal (owner)**
- [ ] Registered business name, registration number and physical address (`SITE_ADDRESS`, required by ECTA s43), plus the rest of the `SITE_*` contact details (phone, WhatsApp, hours, response time). Today they're the showcase's demo text.
- [ ] Food business compliance: a **Certificate of Acceptability** for the kitchen (R638, from the municipality) before selling food to the public.
- [ ] The owner confirms the recipes, allergens and prices for all 12 cookies, including the chocolate suppliers' allergens and cross-contact. Don't invent nutrition values.
- [ ] Get a legal check on the "Diabetic friendly" category name (R146: no implied disease claims).
- [ ] Review the 11 generated cookie images (only Double Chocolate Chunk was approved individually); the photos must show the real product.
- [ ] Review the privacy, terms, refund and cookie policies, with the real business details. POPIA: name the **information officer** and register them with the Information Regulator.
- [ ] VAT: leave `VAT_REGISTERED=false` unless the business is VAT-registered (then receipts show the VAT portion; check prices are VAT-inclusive).
- [ ] Decide the shop rules for real: minimum order (R300), free delivery (R750), pack prices (section 4b), delivery days.

**Accounts to switch from sandbox to live (owner, then Claude sets them)**
- [ ] **PayFast live** account (business and bank details verified by PayFast), then its merchant ID, key and passphrase on production with `PAYFAST_SANDBOX=false`. Set the same passphrase in the PayFast live dashboard.
- [ ] **ShipLogic / The Courier Guy live** account and key, measured box sizes, the real collection address, `SHIPLOGIC_TRACKING_URL` and `SHIPLOGIC_BOOKING_DAYS` back to their defaults, and the tracking privacy settings repeated on the live account (section 4).
- [ ] **Google Places**: upgrade to a full account before **5 February 2027** and set the daily quotas ("Next" section 6).
- [ ] **Azure for Students credit**: check the remaining credit and its expiry date in the portal (Cost Management). When it runs out the API and database stop; a live shop needs a pay-as-you-go subscription or another host before then.

**Keys that were pasted in chat (owner)**
- [ ] Rotate the **Brevo API key** ("Next" section 4) and the **Google Places key** ("Next" section 6) before going live.

**Switching production from showcase to live (Claude, with the owner)**
- [ ] Remove `showcase` from `SPRING_PROFILES_ACTIVE` (keep `prod`) and set `VITE_DEMO_MODE=false` on Vercel production, then redeploy both. That also turns off the demo inbox capture and the simulated payment page.
- [ ] Then remove the production overrides that only existed to beat `showcase`: `APP_MAIL_ENABLED`, `MANAGEMENT_HEALTH_MAIL_ENABLED`, `APP_SITE_SUPPORTEMAIL` (set `SITE_SUPPORT_EMAIL=hello@guiltfreecookie.me` instead) and `DEMO_SIMULATED_PAYMENTS`.
- [ ] **Pickup point**: showcase fills it with "Cape Town (illustrative collection point)". Set the real collection address and instructions in **Admin → Delivery**, or switch pickup off.
- [ ] **Email flows**: `FLOWS_ENABLED=true` when marketing email should start; unpause the **Daily flows** heartbeat in Better Stack and check it turns green after the next 09:00 run.
- [ ] **Clean the production database** of everything from testing: test orders (for example `GFC-261106-JGXY`), test customers and reviews, the demo discount codes `DEMO10` and `12R12`, and any demo admin accounts. Give the remaining admins strong, unique passwords.
- [ ] **Real stock counts** for every cookie (`scripts/restock.sh` sets one number for all; adjust per cookie in **Admin → Stock & packs**).
- [ ] Staging stays on the sandboxes and the demo inbox, and must never get the live PayFast or ShipLogic keys.

**Final checks (owner and Claude)**
- [ ] Real-card test order end to end: payment, Paid status, confirmation email, courier booking with the live account, shipped email with a working tracking link, then a refund recorded in admin and paid back in the PayFast dashboard.
- [ ] Sign up for marketing email and test unsubscribing from it.
- [ ] Brevo: after a few weeks of clean DMARC reports, move DMARC from `p=none` to `p=quarantine`. Watch the free plan's **300 emails a day**.
- [ ] Hosting capacity: staging and production share one small B1 plan (slow restarts, memory at 85–90%). For a live shop, stop staging when it's not in use, or move to B2.
- [ ] Better Stack and Sentry alerts go to the owner's Gmail; make sure someone reads them.

## 6. Smaller loose ends

- [x] Check the shipped mobile hero on a real iPhone / Safari: user reported smooth scrolling with nothing cut off (3 November 2026). Phones currently use the static hero.
- [x] Complete the standalone iPhone / Safari decoder check (4 November 2026, `safari-seek-check-v4`): all eight decoded-image checks passed, including rapid/reverse seeks, at 29–35ms. One compositor callback was absent despite matching pixels (`PIXELS_PASS`); this is decoder validation, not phone cinematic-scroll FPS certification. See [hero scroll validation](hero-scroll-validation.md).

## 7. Shut down after the presentation

The owner plans to shut everything down after the presentation (decided 6 November 2026). In this order, so nothing keeps charging or keeps failing:

1. **Azure** (stops the student credit): delete resource group `rg-guiltfreecookie` (`az group delete -n rg-guiltfreecookie`). That removes both App Services (`guiltfreecookie-api`, `guiltfreecookie-api-staging`), the plan `asp-guiltfreecookie`, MySQL `gfc-mysql` with both databases, and the budget `gfc-monthly-15`. Export anything worth keeping first (for example `mysqldump` of `guiltfreecookie`); the 7-day backups go with the server.
2. **GitHub** (`YaseenKannemeyer/GuiltFreeCookie`): disable the **Verify and deploy API** workflow (or it fails on every push), and delete the `staging` and `production` environments with their `AZURE_WEBAPP_PUBLISH_PROFILE` secrets.
3. **Vercel**: delete the project `guilt-free-cookie` (storefront, staging previews, environment variables) and remove the domain `guiltfreecookie.me` with its DNS records.
4. **Namecheap**: switch **auto-renew off** for `guiltfreecookie.me` (expires 6 November 2027) and for the SSL certificate if one came with the order. The domain can't be deleted early; it lapses.
5. **Google Cloud**: shut down project `guiltfreecookie` (IAM & Admin → Settings → Shut down), which deletes the Places key; then close the billing account or remove the card so nothing can be charged after the trial.
6. **Brevo**: deactivate the API, MCP and SMTP keys, remove the domain and sender, then delete the account (Settings → Account → Delete).
7. **ImprovMX**: remove the domain `guiltfreecookie.me` and delete the account.
8. **ShipLogic sandbox** (`GUI001`): delete the API key `guiltfreecookie-sandbox`; cancel any open test shipments.
9. **Sentry** (org `guilt-free-cookie`): delete the projects `guiltfreecookie-api` and `guiltfreecookie-web`, revoke the `gfc sentry token`, then delete the organisation.
9b. **Grafana Cloud** (org and stack `broadtarantula971`): in the Cloud Portal, delete the access token `1910869-guiltfreecookie-api` (Access Policies), delete the stack, then delete the organisation (My Account / Org Settings). No card is on file, so nothing bills.
10. **Better Stack**: delete the three monitors (Storefront, API, Dependencies) and the two heartbeats (Reservations, Daily flows), then the account.
11. **PayFast sandbox**: nothing charges; optionally delete the sandbox account.
12. **Mac Keychain**: delete the `gfc …` entries (`security delete-generic-password -s "<name>"` for `gfc sentry token`, `gfc brevo api key`, `gfc brevo smtp key`, `gfc shiplogic sandbox key`, `gfc google places key`, `gfc grafana otlp auth`, and the database passwords if saved there).

## 8. Sites and dashboards: what each one is for

**The shop itself**

| Site | What it is | What to look at |
|---|---|---|
| [guiltfreecookie.me](https://guiltfreecookie.me/) | **Production storefront** (the real address; `www.` redirects here) | The live shop: browse, checkout, admin at `/admin` |
| [guilt-free-cookie.vercel.app](https://guilt-free-cookie.vercel.app/) | Production storefront on its old Vercel address | Same build as `guiltfreecookie.me`; still works (allowed by CORS) |
| [Staging storefront](https://guilt-free-cooki-git-7d1aa2-mogamat-yaseen-kannemeyers-projects.vercel.app/) | **Staging**: the `staging` branch, against the staging API and database | Try changes before they reach production; emails stay in the demo inbox |
| [Production API health](https://guiltfreecookie-api.azurewebsites.net/guiltFreeCookie/actuator/health) | The production API on Azure | `UP` means running; `/actuator/health/dependencies` checks Brevo mail, the email queue and ShipLogic |
| [Staging API health](https://guiltfreecookie-api-staging.azurewebsites.net/guiltFreeCookie/actuator/health) | The staging API on Azure | As above; staging also has API docs at `/swagger-ui` |

**Hosting**

| Site | Used for | What it tracks |
|---|---|---|
| [Vercel project](https://vercel.com/mogamat-yaseen-kannemeyers-projects/guilt-free-cookie) | Hosting the storefront; DNS for `guiltfreecookie.me` | Every deployment and its build log (production from `main`, previews from `staging`), environment variables, the domain and its DNS records |
| [Azure portal](https://portal.azure.com/) | Hosting the API and MySQL (resource group `rg-guiltfreecookie`) | Both App Services (settings, restarts, log stream, CPU and memory), MySQL server `gfc-mysql` and its backups, student-credit spend and the $15 budget alert |
| [GitHub](https://github.com/YaseenKannemeyer/GuiltFreeCookie) | Code, pull requests and API deploys | **Actions**: tests on every push, deploy to staging on `staging`, the manual production deploy |
| [Namecheap](https://ap.www.namecheap.com/dashboard) | Registrar for `guiltfreecookie.me` | Expiry (6 November 2027), auto-renew, domain privacy, the nameservers (point to Vercel) |

**Monitoring**

| Site | Used for | What it tracks |
|---|---|---|
| [Better Stack](https://uptime.betterstack.com/) (alerts arrive as [incidents](https://incidents.betterstack.com/)) | Uptime: is the shop up? | **Monitors** every 3 minutes: Storefront (`guiltfreecookie.me`), API health, Dependencies (Brevo mail, email queue, ShipLogic). **Heartbeats**: Reservations (every 5 minutes) and Daily flows (paused while `showcase` is on). Emails `yaseenkannemeyer@gmail.com` when something goes down |
| [Grafana Cloud](https://broadtarantula971.grafana.net) (portal: [grafana.com/orgs/broadtarantula971](https://grafana.com/orgs/broadtarantula971)) | Charts: how busy and how healthy | Metrics the API pushes every minute: requests, errors, response times, JVM memory, database pool, orders, revenue, address searches, courier quotes and bookings, emails. Kept 14 days on the free tier |
| [Sentry](https://guilt-free-cookie.sentry.io/) | Errors: what broke and where | Exceptions from the API (`guiltfreecookie-api`) and the browser (`guiltfreecookie-web`), split by `production` and `staging`; emails on new issues. No personal data |

**Services the shop uses**

| Site | Used for | What it tracks |
|---|---|---|
| [PayFast sandbox](https://sandbox.payfast.co.za/dashboard) | Card payments (test mode) | Every test payment, its notification to the API (`Completed · Success` or an error), refunds |
| [ShipLogic sandbox](https://sandbox.shiplogic.com/) | Courier quotes and bookings (The Courier Guy, test mode) | Booked shipments and their status, waybills, the account balance, tracking privacy settings |
| [Brevo](https://app.brevo.com/) | **Sending** email from `hello@guiltfreecookie.me` | Transactional logs (each email: delivered, bounced, opened), the 300-a-day allowance, the authenticated domain, API and SMTP keys |
| [ImprovMX](https://app.improvmx.com/) | **Receiving** email at `hello@guiltfreecookie.me` | Forwarding to Gmail, the aliases, a log of forwarded mail |
| [Google Cloud console](https://console.cloud.google.com/) (project `guiltfreecookie`) | Address search at checkout (Places API) | API usage and quotas, the key's restrictions, billing (free trial until 5 February 2027) and the budget alert |

