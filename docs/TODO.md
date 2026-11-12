# What's left

Storefront on **Vercel** (`guiltfreecookie.me`), API and MySQL on **Azure for Students**. It costs $0 cash; the App Service uses about $17.74/month of student credit and MySQL is on the student free tier. Client costs: [COSTS.md](COSTS.md); release steps: [WORKFLOW.md](WORKFLOW.md); manual checks: [SMOKE-TEST.md](SMOKE-TEST.md). Both environments still run as a showcase (`prod,showcase`: PayFast and ShipLogic sandboxes, captured inbox).

## Open now: pick up here (stopped 7 November 2026, 00:50 SAST)

In order. **(you)** = the owner, **(Claude)** = Claude.

**Where it stopped**
1–3. **Done 7 November: staging metrics in Grafana.** Staging sends metrics (OTLP connect timeout 10 s; container logging off again). Azure Monitor data source `Azure (guiltfreecookie)` saved (the "old version of datasource" error came from pressing **Load Subscriptions**, which saves in the background; reload, then **Save & test**; "No Log Analytics workspaces" is expected). Dashboard imported; the Shop and Courier rows show data since `21ef027` (every shop counter starts at zero).
4. **Production metrics on (7 November)**: `OTLP_*` settings on production (07:00 UTC restart), then `main` deployed to production (run 42349828788, up at 07:15 UTC). **(you)** In Grafana switch **Environment** to `production` and save the dashboard.
5. **Monitor every site in Grafana** (owner: yes, where free and easy; 7 November). Plan:
   - **Synthetic Monitoring** (free tier; check its monthly allowance on the Synthetics page), one probe, every 5 minutes, about 60,000 runs a month for 7 checks: HTTP checks on `guiltfreecookie.me`, the staging storefront, both APIs' `/guiltFreeCookie/actuator/health` and production's `/health/dependencies` (HTTP checks also report SSL expiry), and a DNS MX check on `guiltfreecookie.me` for ImprovMX. **(you)** turn on Synthetic Monitoring (**Testing & synthetics → Synthetics → Get started**); **(Claude)** gives the check list to enter.
   - **Sentry** data source (free plugin; needs a new read-only Sentry token: Organization, Project and Issue & Event **Read**).
   - **GitHub** data source (free plugin; a fine-grained read-only token for this repo: Actions, Contents, Pull requests **Read**) for deploy runs and failures.
   - **PayFast notifications** metric (`gfc.payfast.notifications` by result; **(Claude)** small code change, goes out with the next release).
   - One dashboard row per service (**(Claude)**).
   - Skipped as not easy: Vercel (no Grafana data source; the storefront checks cover it) and Brevo statistics (the `mail` dependency check already shows if sending breaks).
6. **Status page live**: [status.guiltfreecookie.me](https://status.guiltfreecookie.me) (7 November; Better Stack, CNAME `status` → `statuspage.betteruptime.com` in Vercel DNS, HTTPS answers 200). **(you)** In **Structure**, give the three resources public names (e.g. Shop, Ordering system, Payments, email and courier) and save.

**Before the presentation**
7. **(you) Restock production**: `scripts/restock.sh production` (tops every cookie up to 10,000 by default since 7 November; asks for the admin email and password). Run it while production is up, not during a deploy.
8. **(you) Staging → Admin → Delivery: PUDO off** (production already off; free delivery from R750 checked on staging).

**Database**
- **(you, when convenient) Upgrade MySQL 8.0 → 8.4** (`gfc-mysql`, Azure portal → **Upgrade**; take a backup first, and do it outside the presentation week). 8.0 is on Azure's paid extended support ($0.10/vCore-hour, ≈ $73/month); free today only because of the student free tier, but it's the biggest cost if a client takes this over ([COSTS.md](COSTS.md)). Restarts the database, so both APIs lose it for a few minutes.

**Housekeeping**
9. **(you) Commit** what's left: this file, `docs/grafana/` and the OTLP connect timeout in `application.properties` (the Grafana code itself went out in `4540898`). The modified `frontend/` files belong to the frontend work, not this session.
10. **(you) Secrets pasted in chat on 6–7 November**: the Brevo API key, the Grafana OTLP token and the Azure reader secret. All are scoped and go away at shut-down (section 7); replace them earlier if this chat is shared.
11. **(you) Google card hold**: check the bank statement around **20 November 2026**; if the R847.81 hold is still there, ask Google (**Billing → Help → Contact billing support**) or the bank.

**Done on 6–7 November**: `guiltfreecookie.me` live (Vercel, `www` redirect, canonical links and sitemap); production sends real email from `hello@guiltfreecookie.me` through Brevo (SPF, DKIM and DMARC pass, mail-tester 8.6/10) and receives it through ImprovMX; courier booking on production (sandbox); Better Stack storefront monitor on the new domain; R750 free delivery; Grafana Cloud with the API's metrics code deployed to staging and an Azure read-only identity.

## Hardening backlog (added 7 November 2026)

Most important first. `[x]` = already in the code (checked 7 November), `[~]` = partly done, `[ ]` = open. **(you)** = owner, **(Claude)** = Claude.

### P1. Account takeover and leaked data

- [ ] **Login tokens are readable by any script on the page.** Admin and customer JWTs sit in `localStorage`/`sessionStorage` ([useAdminAuth.js](../frontend/src/composables/useAdminAuth.js), [useCustomerAuth.js](../frontend/src/composables/useCustomerAuth.js)). Move them to an `HttpOnly; Secure; SameSite` cookie set by the API, add CSRF protection, and drop the token from the JS. Vercel and Azure are on different domains, so the cookie needs the API on a subdomain (e.g. `api.guiltfreecookie.me`) or a Vercel rewrite to the API. **(Claude)**
- [x] **Admin rights are checked on the server.** `SecurityConfig` requires `ROLE_ADMIN` from the JWT for every admin endpoint; the router's `requiresAdmin` check only hides pages. Keep it that way: nothing in the browser decides who is an admin.
- [ ] **Second step at login (2FA)**: TOTP (authenticator app) for admins first, with recovery codes; optional for customers. **(Claude)**
- [~] **Stop password guessing.** Done: per-IP limit of 10 tries per 5 minutes on admin login and customer auth (`RateLimitFilter`). Open: the counters live in memory (reset on restart, per instance) and nothing limits tries per *account*. Add a per-account lockout with backoff, and an email to the owner of the account when it locks. **(Claude)**
- [ ] **Leaked-password check**: on register, reset and change password, reject passwords found in Have I Been Pwned (k-anonymity range API, only a 5-character hash prefix leaves the server). Also set a minimum length of 12. **(Claude)**

### P1. Legal blockers before taking real orders

- [ ] **Terms and privacy checkbox** at sign-up and checkout (today checkout only says "By paying you agree"), unticked by default, required. **(Claude)**
- [ ] **Record acceptance**: store who accepted which terms version and when (customer, order, timestamp, version). **(Claude)**
- [x] Privacy policy, terms, refund policy and cookie policy pages exist ([views/legal](../frontend/src/views/legal)). Real business details and a legal review are still open in section 5.
- [x] Separate, unticked **marketing consent** at sign-up and checkout.
- [x] **Unsubscribe** in marketing email: one-click `List-Unsubscribe` (RFC 8058) and the email preferences page.
- [ ] **Data access and deletion requests**: today the privacy policy says "email us". Add "Download my data" and "Delete my account" in **Account** (delete anonymises orders, which have to be kept for tax). **(Claude)**
- [ ] **Data retention limits**: only the email outbox is cleaned today. Decide and enforce limits: abandoned carts, contact messages, product-view events, password reset tokens, unsubscribed subscribers, inactive accounts. State them in the privacy policy. **(you decide, Claude builds)**
- [ ] **Subprocessors list** in the privacy policy (only Google is named): Azure, Vercel, Brevo, ImprovMX, PayFast, ShipLogic/The Courier Guy, Google Places, Sentry, Better Stack, Grafana Cloud, with what each gets and where. **(Claude drafts, you check)**
- [ ] **Data processing agreements**: accept each provider's DPA (most are click-through in their dashboards). **(you)**
- [ ] **Business contact details** (`SITE_ADDRESS`, phone, registration number). Same as section 5. **(you)**
- [ ] **Refund policy**: the page exists; check it matches what the shop will actually do (CPA, ECTA cooling-off doesn't apply to perishable food). **(you)**

### P2. Email on production

- [x] Production sends real email (Brevo); order confirmation, shipped, ready to collect, customer cancellation and the courier booking email work.
- [ ] **An email at every order step.** Missing today: **Processing** ("we've started baking"), **Delivered**, **Refunded**, **Payment failed**, and **cancelled by the shop** (only customer cancellations send one). `OrderEvents.Delivered` exists but only starts the post-purchase flow. Add events and templates in `OrderEmailListener`. **(Claude)**
- [ ] **Admin password reset sends an email.** `POST /admin/resetPassword/{id}` returns a temporary password to the screen instead. Email the admin a reset link (the customer forgot-password email already works); add "Forgot password" on the admin login. **(Claude)**
- [ ] **Discount code email**: nothing emails a code today. Add "send this code to…" in **Admin → Discounts**, and include the code in the welcome / win-back flow emails where they offer one. **(Claude)**
- [ ] **Retention flows on production**: welcome, browse and cart abandonment, post-purchase, replenishment, win-back and sunset exist but `showcase` turns them off. Set `FLOWS_ENABLED=true` on production, unpause the Better Stack heartbeat, then trigger each flow once and check it arrives (section 5 has the same switch). **(Claude, with you)**
- [ ] Watch Brevo's free plan (300 emails a day) once flows are on.

### P2. Security docs (in `docs/`)

- [ ] `AUTH.md`: sign-up, login, sessions, password reset and who can reach which endpoint. **(Claude)**
- [ ] `SECURITY.md`: rules no agent may break: no secrets in code, validate every input, rate limit every public endpoint, no PII in logs. **(Claude)**
- [ ] `RLS-POLICIES.md`: which rows each role can read and change. MySQL has no row-level security, so this documents the checks the services make (a customer only sees their own orders and addresses), with a test per rule. **(Claude)**
- [ ] `ENV.md`: every key and secret, what it does and where it lives (Azure app settings, Vercel, Keychain). Names only, never values. **(Claude)**
- [ ] `THREAT-MODEL.md`: what is protected, from whom, and how, aimed at the two worst outcomes: leaked data and a taken-over admin account. **(Claude)**

### P3. Other legal and fairness checks

- [x] No fake countdown timers (the only timers are search delays and undo notices).
- [x] No SOC 2 or similar claims found.
- [x] No subscriptions, so nothing auto-renews and there's nothing to cancel. Revisit if a subscription box is ever added.
- [~] Children: the privacy policy covers under-18s. Add "you must be 18 or have a parent's consent" to the terms. **(Claude)**
- [x] Cookie banner and blocking trackers until consent: not needed today. The cookie policy says no tracking pixels; only essential storage and Sentry (PII off). Add a consent banner **before** adding any analytics or ad pixel.
- [ ] **Audit third-party scripts**: confirm the storefront loads only Sentry and Google Places, and that Sentry sends no PII or session replay. **(Claude)**
- [ ] **Hidden fees**: check delivery cost and any minimum-order rule are shown before the payment page (they are in the cart and checkout today; confirm on mobile). **(you)**
- [ ] **Remove unsupported claims**: "Diabetic friendly", "guilt free", health wording (section 5 has the legal check). **(you)**
- [ ] **Fake reviews**: reviews need a verified purchase, but test reviews on production must be deleted (section 5, "Clean the production database"). **(you)**
- [ ] **Label AI-generated content**: 11 cookie photos are generated. Replace them with real photos before selling, or label them. **(you)**
- [ ] **Colour contrast**: run an axe / Lighthouse accessibility pass on every page and fix anything below WCAG AA. **(Claude)**
- [ ] **Alt text**: 25 `<img>` tags have no `alt` on the same line; check each (decorative images get `alt=""`). **(Claude)**
- [ ] **Keyboard navigation**: add a "Skip to content" link, check focus order and visible focus in checkout, modals and the admin. **(Claude)**
- [ ] **Licences**: fonts, icons, the hero video and the npm and Maven dependencies (run a licence report; keep notices where MIT/Apache require them). **(Claude)**
- [ ] **Customer or brand logos**: none found; keep it that way unless you have written permission. **(you)**
- [ ] **No training on customer data**: say in the privacy policy that customer data isn't used to train AI, and keep AI tools away from the production database. **(Claude drafts)**
- [ ] **Uptime**: don't promise a percentage in the terms; the status page is enough. **(Claude checks the terms)**
- [ ] **Copyright takedowns**: DMCA registration is US-only; South Africa uses ECTA s77 take-down notices. Only matters if customers can upload content (they can't today). **(you, later)**
- [ ] **Dark patterns**: walk the sign-up, checkout and unsubscribe flows for pre-ticked boxes, confirm-shaming or hard-to-find cancel. **(Claude)**

### P3. Speed

- [ ] **N+1 queries**: there are no JPA relations, so N+1 comes from repository calls inside loops (order lists, cart lines, admin views, flows). Find each one (turn on Hibernate statistics on staging) and replace it with one `IN (...)` query. **(Claude)**
- [~] **Database indexes**: 10 tables have them (orders, order items, carts, payments, email tables, inventory). Check the slow queries for the rest (cookies by category and slug, reviews by cookie, newsletter by email). Production runs `ddl-auto=validate`, so new indexes need SQL run by hand on both databases. **(Claude)**
- [ ] **Caching**: Redis is a paid add-on on Azure. Start with an in-memory cache (Caffeine) for the catalogue, site info and shipping settings, cleared when admin edits them. Move to Redis only if there's ever more than one API instance. **(Claude)**
- [~] **Lazy-load images**: 11 of 30 `<img>` tags have `loading="lazy"`. Add it to everything below the fold; keep the hero eager. **(Claude)**
- [~] **Update the UI before the server answers** (optimistic UI): undo notices exist in a few places. Add it to cart quantity changes, add-to-cart and admin toggles, with a roll-back on error. **(Claude)**

### P4. Search (rank on Google)

Already in the code: prerendered pages, `sitemap.xml`, `robots.txt`, `llms.txt`, canonical links, and Bakery, Product, FAQ and breadcrumb structured data.

- [ ] **Google Search Console**: verify `guiltfreecookie.me` (DNS TXT in Vercel), submit the sitemap, then share the queries and pages report. Claude can't log in to it. **(you)**
- [ ] **Google Business Profile**: needs the real business address or a service area. It's the main way to rank locally. **(you)**
- [ ] **Keyword Planner, Google Trends, AnswerThePublic, Ubersuggest**: export the results for "sugar free cookies", "keto cookies", "diabetic cookies" and "cookie delivery Cape Town" (CSV), and Claude turns them into page and FAQ changes. **(you export, Claude acts)**
- [ ] **Screaming Frog** (free up to 500 URLs): crawl the site and share the export; Claude fixes broken links, missing titles and duplicates. **(you)**
- [ ] **PageSpeed Insights**: run the home, shop and a product page on mobile; Claude fixes what it flags. **(Claude can run it)**
- [ ] **Rich Results Test**: check the product and FAQ pages show as eligible (price, availability, rating). **(Claude can run it)**
- [ ] **Yoast** is a WordPress plugin, so it doesn't apply. The same checks (titles, meta descriptions, one H1, internal links) come from the Screaming Frog crawl.

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
- [x] Alias **`hello`** → `yaseenkannemeyer@gmail.com` (the alias box takes only the part before the `@`). A catch-all `*` alias can be added back so any other address also arrives.
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

**What each monitor does** (all alerts email `yaseenkannemeyer@gmail.com`)

| Monitor | Where | What it checks | Goes red when | Production only? |
|---|---|---|---|---|
| **Storefront** | Better Stack, every 3 min | Loads `https://guiltfreecookie.me/` | The shop page doesn't load (Vercel down, DNS or SSL broken) | Yes |
| **API** | Better Stack, every 3 min | `/guiltFreeCookie/actuator/health`: the API is running and can reach MySQL | The API is down or restarting for more than 5 minutes, or the database is unreachable | Yes |
| **Dependencies** | Better Stack, every 3 min | `/actuator/health/dependencies`: Brevo accepts mail (`mail`), emails aren't stuck in the queue (`outbox`), ShipLogic answers (`delivery`) | Email or courier booking is broken even though the API is up | Yes |
| **Reservations** heartbeat | Better Stack, the API pings it | The 5-minute job that frees stock held by unpaid checkouts has run | No ping for 15 minutes: the job stopped or the API is down, so stock stays locked | Yes |
| **Daily flows** heartbeat | Better Stack, the API pings it | The 09:00 marketing email job (abandoned carts, reviews, win-back) has run | No ping for 25 hours. **Always red while `showcase` is on**, because that profile turns the job off. Switched back on by the owner on 7 November; pause it to stop the daily alert, or leave it on knowing it will alert | Yes |
| **Sentry** `guiltfreecookie-api` / `-web` | Sentry | Errors thrown by the API and by the shop in visitors' browsers, with where they happened | A new kind of error appears | Both (tagged `production` / `staging`) |
| **Grafana dashboard** | Grafana Cloud | Orders, revenue, emails, courier quotes and bookings, address searches, request rates and errors, memory, database pool, and Azure's CPU, memory, restarts and MySQL load | Nothing alerts yet; it's for looking at | Staging now, production after "Open now" 4 |
| **Grafana synthetic checks** (planned, "Open now" 5) | Grafana Cloud | Both storefronts and both APIs load, SSL isn't about to expire, the MX record for ImprovMX is there | Any of those fails | Both |

- [x] **Heartbeats built.** `Heartbeat` GETs a URL in the background (3 s timeout, logs failures, never throws, does nothing when the URL is blank). Pinged at the end of `ReservationReleaseJob` (every 5 minutes) and of the daily 09:00 cron in `FlowTriggers`. A job that throws doesn't ping.
- [x] **Sentry built** (errors only, no personal data, replays or tracing).
  - Backend: `sentry-spring-boot-4-starter` 8.61.0, configured by `SENTRY_DSN`, `SENTRY_ENVIRONMENT` and `SENTRY_SEND_DEFAULT_PII` (default `false`). A blank DSN turns it off.
  - Frontend: `@sentry/vue` 11.8.0 in `src/monitoring.js`, loaded as a separate chunk only when `VITE_SENTRY_DSN` is set (builds without it ship no Sentry code). The DSN's host is added to `connect-src` in the `<meta>` policy; the `vercel.json` header allows Sentry's ingest hosts.
- [x] **Better Stack** (free): three uptime monitors at 3-minute intervals, default timeout, 3-minute recovery period, 5-minute confirmation period (longer than a cold start), regions Europe and North America
  - [ ] Storefront: `https://guilt-free-cookie.vercel.app/` (made during onboarding); change to `https://guiltfreecookie.me/` ("Open now" 3)
  - [x] API: `https://guiltfreecookie-api.azurewebsites.net/guiltFreeCookie/actuator/health`
  - [x] Dependencies: `…/guiltFreeCookie/actuator/health/dependencies` (mail and outbox), created 6 November 2026
- [x] Better Stack heartbeats (2026-11-04, URLs set on production): create two, "Reservations" (expected every 5 minutes, grace 10) and "Daily flows" (every 24 hours, grace 1 hour). Put their URLs in `HEARTBEAT_RESERVATIONS_URL` and `HEARTBEAT_DAILY_FLOWS_URL` on the production App Service.
- [x] **Public status page** (Better Stack, free, 7 November 2026): [status.guiltfreecookie.me](https://status.guiltfreecookie.me), built from the three monitors (no heartbeats). DNS: CNAME `status` → `statuspage.betteruptime.com` in Vercel.
- [ ] **Grafana Cloud dashboards** (free tier; code built 6 November 2026, not yet deployed).
  - [x] Code: `micrometer-registry-otlp` plus `spring-boot-opentelemetry` (Spring Boot 4 only switches the OTLP exporter on when that module is present). Every minute the API pushes request counts and times, errors, JVM memory, the database pool, and shop numbers from `ShopMetrics`: `gfc.orders.placed`, `gfc.orders.paid`, `gfc.revenue`, `gfc.address.searches`, `gfc.courier.quotes` (live or fallback), `gfc.courier.bookings`, `gfc.emails` (sent, failed, expired). Tags `application=guiltfreecookie-api` and `environment` (from `SENTRY_ENVIRONMENT`). Off unless `OTLP_METRICS_ENABLED=true`. Checked locally against a stand-in receiver (batches arrive with the `Authorization` header); `ShopMetricsTest`; all 490 tests pass.
  - [x] Grafana Cloud account (free tier; a 14-day trial of extra features ends 20 November 2026 and drops back to free; no card added). Org and stack `broadtarantula971` ([broadtarantula971.grafana.net](https://broadtarantula971.grafana.net)), region EU West. Token `1910869-guiltfreecookie-api` from **Cloud Portal → stack Details → OpenTelemetry → Configure**, saved as `gfc grafana otlp auth` (the whole `Basic …` value, 246 characters; it was pasted in chat). Endpoint `https://otlp-gateway-prod-eu-west-6.grafana.net/otlp`.
  - [x] Checked 7 November 2026: the token is accepted (200, 401 without it), and a local run sent metrics to Grafana Cloud with no failures (tagged `environment=local`). In Grafana: **Explore → Prometheus → `jvm_memory_used_bytes`**.
  - [ ] Settings on staging, then production: `OTLP_METRICS_ENABLED=true`, `OTLP_METRICS_URL=https://otlp-gateway-prod-eu-west-6.grafana.net/otlp/v1/metrics`, `OTLP_METRICS_AUTHORIZATION=<gfc grafana otlp auth>`. Then deploy (commit, push to `staging`, merge to `main`, run the production deploy), one app at a time.
  - [x] Staging: `OTLP_*` settings on (7 November 2026).
  - [x] Dashboard `docs/grafana/guiltfreecookie-dashboard.json` (Shop, Courier and address search, API health, Azure). Import: **Dashboards → New → Import → Upload dashboard JSON file**; it asks for the Prometheus and Azure Monitor data sources.
  - [x] **Azure metrics**: read-only identity `gfc-grafana-reader` (appId `3044f590-d46a-4269-b060-6fa28c206517`, tenant `cc6148eb-d356-4f38-900f-3a4d62b954c8`), role **Monitoring Reader** on `rg-guiltfreecookie` only; secret saved as `gfc grafana azure secret` (checked: it reads the plan's CPU). Creating it needs a fresh `az login` (CPUT asks for a sign-in within 12 hours) and was run by the owner.
  - [ ] Owner: add the **Azure Monitor** data source in Grafana (App Registration: tenant ID, client ID, client secret from Keychain), then import the dashboard.
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
9b. **Grafana Cloud** (org and stack `broadtarantula971`): first remove the Azure reader identity (`az ad sp delete --id 3044f590-d46a-4269-b060-6fa28c206517`, unless the whole resource group is already gone). Then, in the Cloud Portal, delete the access token `1910869-guiltfreecookie-api` (Access Policies), delete the stack, then delete the organisation (My Account / Org Settings). No card is on file, so nothing bills.
10. **Better Stack**: delete the three monitors (Storefront, API, Dependencies) and the two heartbeats (Reservations, Daily flows), then the account.
11. **PayFast sandbox**: nothing charges; optionally delete the sandbox account.
12. **Mac Keychain**: delete the `gfc …` entries (`security delete-generic-password -s "<name>"` for `gfc sentry token`, `gfc brevo api key`, `gfc brevo smtp key`, `gfc shiplogic sandbox key`, `gfc google places key`, `gfc grafana otlp auth`, `gfc grafana azure secret`, and the database passwords if saved there).

## 8. Sites and dashboards: what each one is for

**API called** = which API that site talks to, or which of our endpoints talks to it. **Code** = the file in this repo that makes or answers that call. **Monitors** = what you can watch there.

**The shop itself**

| Site | What it is | API called | Code | Monitors |
|---|---|---|---|---|
| [guiltfreecookie.me](https://guiltfreecookie.me/) | **Production storefront** (`www.` redirects here) | The browser calls the production API `https://guiltfreecookie-api.azurewebsites.net/guiltFreeCookie/api/v1/…` (catalogue, carts, checkout, address search, orders, admin), and Sentry's ingest for browser errors | [http.js](../frontend/src/api/http.js) (base URL `VITE_API_BASE_URL`), the calls in [storeApi.js](../frontend/src/api/storeApi.js), [cookieApi.js](../frontend/src/api/cookieApi.js), [payment.js](../frontend/src/api/payment.js), [adminApi.js](../frontend/src/api/adminApi.js); Sentry in [monitoring.js](../frontend/src/monitoring.js) | Nothing itself; watched by Better Stack (Storefront monitor), Sentry (`guiltfreecookie-web`) and, once set up, Grafana synthetic checks |
| [guilt-free-cookie.vercel.app](https://guilt-free-cookie.vercel.app/) | Production storefront on its old Vercel address | Same production API (allowed by CORS) | Same as above; CORS list in [SecurityConfig.java](../src/main/java/za/ac/cput/guiltfreecookie/config/SecurityConfig.java) | As above |
| [Staging storefront](https://guilt-free-cooki-git-7d1aa2-mogamat-yaseen-kannemeyers-projects.vercel.app/) | **Staging**: the `staging` branch | The staging API `https://guiltfreecookie-api-staging.azurewebsites.net/guiltFreeCookie/api/v1/…` | [http.js](../frontend/src/api/http.js) with the staging `VITE_API_BASE_URL` | Sentry (`guiltfreecookie-web`, environment `staging`) |
| [Production API](https://guiltfreecookie-api.azurewebsites.net/guiltFreeCookie/actuator/health) | The Spring Boot API on Azure | **Calls out to:** MySQL `gfc-mysql.mysql.database.azure.com:3306`; PayFast `…/eng/process` (payment page) and `…/eng/query/validate` (checks each notification); ShipLogic `api.shiplogic.com/v2/rates`, `/v2/shipments`, `/v2/shipments/label`, `/v2/tracking/shipments`; Google `places.googleapis.com/v1/places:autocomplete` and `/v1/places/{id}`; Brevo SMTP `smtp-relay.brevo.com:587`; Sentry ingest; Grafana `otlp-gateway-prod-eu-west-6.grafana.net/otlp/v1/metrics`; Better Stack heartbeat URLs. **Called by:** the storefronts (`/api/v1/…`), PayFast (`/api/v1/payments/payfast/notify`), Better Stack (`/actuator/health`, `/actuator/health/dependencies`) | MySQL: [application.properties](../src/main/resources/application.properties) (`spring.datasource`); PayFast: [PayFastGateway.java](../src/main/java/za/ac/cput/guiltfreecookie/service/payment/PayFastGateway.java), [PayFastVerifier.java](../src/main/java/za/ac/cput/guiltfreecookie/service/payment/PayFastVerifier.java), [PayFastController.java](../src/main/java/za/ac/cput/guiltfreecookie/controller/PayFastController.java); ShipLogic: [ShipLogicClient.java](../src/main/java/za/ac/cput/guiltfreecookie/service/shipping/ShipLogicClient.java), [LiveShippingProvider.java](../src/main/java/za/ac/cput/guiltfreecookie/service/shipping/LiveShippingProvider.java), [CourierBookingService.java](../src/main/java/za/ac/cput/guiltfreecookie/service/order/CourierBookingService.java); Google: [AddressSearchService.java](../src/main/java/za/ac/cput/guiltfreecookie/service/address/AddressSearchService.java); Brevo: [EmailService.java](../src/main/java/za/ac/cput/guiltfreecookie/service/notification/EmailService.java), [EmailOutbox.java](../src/main/java/za/ac/cput/guiltfreecookie/service/notification/EmailOutbox.java); Grafana: [ShopMetrics.java](../src/main/java/za/ac/cput/guiltfreecookie/util/ShopMetrics.java); heartbeats: [Heartbeat.java](../src/main/java/za/ac/cput/guiltfreecookie/service/monitoring/Heartbeat.java); health: [OutboxHealthIndicator.java](../src/main/java/za/ac/cput/guiltfreecookie/config/OutboxHealthIndicator.java), [DeliveryHealthIndicator.java](../src/main/java/za/ac/cput/guiltfreecookie/config/DeliveryHealthIndicator.java) | `/actuator/health`: running and database; `/actuator/health/dependencies`: Brevo mail (`mail`), the email queue (`outbox`), ShipLogic (`delivery`) |
| [Staging API](https://guiltfreecookie-api-staging.azurewebsites.net/guiltFreeCookie/actuator/health) | The same API for staging | Same services, with the sandbox keys; mail stays in the demo inbox. API docs at `/swagger-ui` | Same files; settings differ only in Azure | As above |

**Hosting**

| Site | Used for | API called | Code | Monitors |
|---|---|---|---|---|
| [Vercel project](https://vercel.com/mogamat-yaseen-kannemeyers-projects/guilt-free-cookie) | Hosting the storefront; DNS for `guiltfreecookie.me` | At build time the prerender step calls the API's `/api/v1/cookies` (`PRERENDER_API_URL`). Claude uses the Vercel CLI (`vercel ls`, `vercel redeploy`, `vercel env`, `vercel dns`) | [vercel.json](../frontend/vercel.json) (headers, CSP), [prerender.mjs](../frontend/scripts/prerender.mjs) (catalogue at build time) | Every deployment and its build log, environment variables, the domain and its DNS records (Brevo DKIM, ImprovMX MX, SPF, DMARC) |
| [Azure portal](https://portal.azure.com/) | Hosting the API and MySQL (resource group `rg-guiltfreecookie`) | GitHub Actions deploys through the publish profile; Claude uses the Azure CLI (`az webapp …`, the Kudu `/api/command` for staging logs) | [deploy-api.yml](../.github/workflows/deploy-api.yml) | Both App Services (settings, restarts, log stream, CPU, memory, startup log), MySQL `gfc-mysql` (CPU, storage, backups), student-credit spend and the $15 budget alert |
| [GitHub](https://github.com/YaseenKannemeyer/GuiltFreeCookie) | Code, pull requests and API deploys | Actions run `mvn verify`, then `azure/webapps-deploy` to Azure; Vercel's GitHub app builds the storefront on every push | [deploy-api.yml](../.github/workflows/deploy-api.yml) | Test and deploy runs (pass or fail), pull requests |
| [Namecheap](https://ap.www.namecheap.com/dashboard) | Registrar for `guiltfreecookie.me` | None; its nameservers hand the domain to Vercel (`ns1/ns2.vercel-dns.com`) | None | Expiry (6 November 2027), auto-renew, domain privacy |

**Monitoring**

| Site | Used for | API called | Code | Monitors |
|---|---|---|---|---|
| [Better Stack](https://uptime.betterstack.com/) (alerts arrive as [incidents](https://incidents.betterstack.com/)) | Uptime and alerts | **Calls:** `https://guiltfreecookie.me/`, the production API's `/actuator/health` and `/actuator/health/dependencies`, every 3 minutes. **Called by:** the API's heartbeat pings (`HEARTBEAT_RESERVATIONS_URL` every 5 minutes, `HEARTBEAT_DAILY_FLOWS_URL` daily) | [Heartbeat.java](../src/main/java/za/ac/cput/guiltfreecookie/service/monitoring/Heartbeat.java), called from [ReservationReleaseJob.java](../src/main/java/za/ac/cput/guiltfreecookie/service/order/ReservationReleaseJob.java) and [FlowTriggers.java](../src/main/java/za/ac/cput/guiltfreecookie/service/flow/FlowTriggers.java); health checks: [OutboxHealthIndicator.java](../src/main/java/za/ac/cput/guiltfreecookie/config/OutboxHealthIndicator.java), [DeliveryHealthIndicator.java](../src/main/java/za/ac/cput/guiltfreecookie/config/DeliveryHealthIndicator.java), groups in [application.properties](../src/main/resources/application.properties) | Storefront up, API up, dependencies (Brevo mail, email queue, ShipLogic) up; Reservations heartbeat (the 5-minute job runs); Daily flows heartbeat (paused while `showcase` is on). Emails `yaseenkannemeyer@gmail.com` on downtime |
| [Grafana Cloud](https://broadtarantula971.grafana.net) (portal: [grafana.com/orgs/broadtarantula971](https://grafana.com/orgs/broadtarantula971)) | Charts and trends | **Called by:** the API, which pushes metrics every minute to `otlp-gateway-prod-eu-west-6.grafana.net/otlp/v1/metrics` (`OTLP_METRICS_*`) | [ShopMetrics.java](../src/main/java/za/ac/cput/guiltfreecookie/util/ShopMetrics.java) (shop numbers), [application.properties](../src/main/resources/application.properties) (`management.otlp.*`), dashboard [guiltfreecookie-dashboard.json](../docs/grafana/guiltfreecookie-dashboard.json) | Requests, errors (5xx), response times, slowest endpoints, JVM memory, database pool, orders placed and paid, revenue, emails (sent, failed, expired), courier quotes (live or fallback) and bookings, address searches. Kept 14 days. Dashboard: `docs/grafana/guiltfreecookie-dashboard.json` |
| [Sentry](https://guilt-free-cookie.sentry.io/) | Errors: what broke and where | **Called by:** the API (`SENTRY_DSN`) and the storefront in the browser (`VITE_SENTRY_DSN`) | [application.properties](../src/main/resources/application.properties) (`sentry.*`, backend starter), [monitoring.js](../frontend/src/monitoring.js) | Exceptions with stack traces in `guiltfreecookie-api` and `guiltfreecookie-web`, split by `production` and `staging`; emails on new issues. No personal data |

**Services the shop uses**

| Site | Used for | API called | Code | Monitors |
|---|---|---|---|---|
| [PayFast sandbox](https://sandbox.payfast.co.za/dashboard) | Card payments (test mode) | **The API calls** `sandbox.payfast.co.za/eng/process` (the customer is sent there to pay) and `/eng/query/validate` (to confirm each notification). **PayFast calls** the API's `/api/v1/payments/payfast/notify` (ITN), from its published IP ranges only | [PayFastGateway.java](../src/main/java/za/ac/cput/guiltfreecookie/service/payment/PayFastGateway.java) (payment form), [PayFastVerifier.java](../src/main/java/za/ac/cput/guiltfreecookie/service/payment/PayFastVerifier.java) (signature, IP ranges, validate), [PayFastController.java](../src/main/java/za/ac/cput/guiltfreecookie/controller/PayFastController.java) (notify) | Every test payment, whether its notification reached the API (`Completed · Success` or an error), refunds |
| [ShipLogic sandbox](https://sandbox.shiplogic.com/) | Courier quotes and bookings (The Courier Guy, test mode) | **The API calls** `api.shiplogic.com`: `/v2/rates` (quote at checkout), `/v2/shipments` (Book courier), `/v2/shipments/label` (waybill), `/v2/tracking/shipments` (every 30 minutes, for Delivered) | [ShipLogicClient.java](../src/main/java/za/ac/cput/guiltfreecookie/service/shipping/ShipLogicClient.java) (all calls), [LiveShippingProvider.java](../src/main/java/za/ac/cput/guiltfreecookie/service/shipping/LiveShippingProvider.java) (quotes), [CourierBookingService.java](../src/main/java/za/ac/cput/guiltfreecookie/service/order/CourierBookingService.java) (booking, waybill, Delivered poll), [DeliveryHealthIndicator.java](../src/main/java/za/ac/cput/guiltfreecookie/config/DeliveryHealthIndicator.java) | Booked shipments and their status, waybills, the account balance, tracking privacy settings |
| [Brevo](https://app.brevo.com/) | **Sending** email from `hello@guiltfreecookie.me` | **The API sends** over SMTP to `smtp-relay.brevo.com:587` (login `c1dacb001@smtp-brevo.com`). Claude uses the Brevo API `api.brevo.com/v3` for setup (domain, sender) | [EmailService.java](../src/main/java/za/ac/cput/guiltfreecookie/service/notification/EmailService.java) (builds emails), [EmailOutbox.java](../src/main/java/za/ac/cput/guiltfreecookie/service/notification/EmailOutbox.java) (queue and SMTP send), [OutboxHealthIndicator.java](../src/main/java/za/ac/cput/guiltfreecookie/config/OutboxHealthIndicator.java) | Transactional log for each email (delivered, bounced, opened, clicked), the 300-a-day allowance, the authenticated domain, API and SMTP keys |
| [ImprovMX](https://app.improvmx.com/) | **Receiving** email at `hello@guiltfreecookie.me` | None from the shop; mail reaches it through the MX records `mx1/mx2.improvmx.com` | None | Forwarding to Gmail, the aliases, a log of forwarded mail |
| [Google Cloud console](https://console.cloud.google.com/) (project `guiltfreecookie`) | Address search at checkout | **The API calls** Places API (New): `places.googleapis.com/v1/places:autocomplete` (suggestions as you type) and `/v1/places/{id}` (the full address) | [AddressSearchService.java](../src/main/java/za/ac/cput/guiltfreecookie/service/address/AddressSearchService.java), [AddressSearchController.java](../src/main/java/za/ac/cput/guiltfreecookie/controller/AddressSearchController.java) | API usage and quotas, the key's restrictions (Places only), billing (free trial until 5 February 2027) and the $5 budget alert |
