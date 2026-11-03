# Email, delivery and monitoring plan

Three services to add to the hosted shop, all free at demo volume:

| Need | Pick | Free tier | Why this one |
|---|---|---|---|
| Email | **Brevo** | 300 emails/day, SMTP included | Works with the existing SMTP code (just new `MAIL_*` values). Handles both order emails and the marketing flows |
| Delivery | **ShipLogic (The Courier Guy)** | Free sandbox; live shipments are paid per waybill | The only South African option with public API docs and a self-serve sandbox. Bob Go's docs are only available through its sales team |
| Monitoring | **Better Stack Uptime** + **Sentry** | 10 monitors at 3-minute checks, 10 heartbeats, 1 status page; Sentry's free developer plan | Better Stack tells you when something is down, including scheduled jobs that stop running. Sentry tells you when something throws an error |

This replaces Gmail SMTP and UptimeRobot from [HOSTING-PLAN.md](HOSTING-PLAN.md). UptimeRobot's terms have changed back and forth on whether the free plan allows commercial use, and its free plan has no heartbeats.

Do them in this order: email first (smallest change), then monitoring, then delivery (most code).

---

## 1. Email: Brevo

No code changes. `EmailService` already sends through Spring's `JavaMailSender`, and `EmailOutbox` handles retries.

### 1.1 Get a domain first

Brevo, Gmail and Outlook all expect the sender's domain to be verified with SPF, DKIM and DMARC records. If you send from a `@gmail.com` address through Brevo, it lands in spam or is rejected.

- Claim a free domain through the [GitHub Student Developer Pack](https://education.github.com/pack) (Namecheap and name.com offers), e.g. `guiltfreecookie.me`.
- Point it at Vercel as well, so the shop and the email share the same domain.

### 1.2 Set up Brevo

1. Sign up at [brevo.com](https://www.brevo.com) on the free plan.
2. **Senders, Domains & Dedicated IPs → Domains → Add a domain.** Add the TXT and CNAME records Brevo shows you (verification code, DKIM, DMARC) at your domain registrar. Wait until all three show as verified.
3. **Senders → Add a sender:** `hello@<your-domain>`.
4. **SMTP & API → SMTP:** generate an SMTP key. If SMTP isn't available on the account yet, ask Brevo support to enable it for transactional email.
5. **Transactional → Settings:** turn off Brevo's open and click tracking. The app already tracks opens and clicks itself (`EmailTrackingService`), and Brevo's link rewriting would wrap your unsubscribe links.

### 1.3 App Service settings

```
MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=587
MAIL_USERNAME=<SMTP login shown on Brevo's SMTP page>
MAIL_PASSWORD=<SMTP key>
MAIL_SMTP_AUTH=true
MAIL_STARTTLS=true
MAIL_FROM=GuiltFreeCookie <hello@<your-domain>>
SITE_SUPPORT_EMAIL=hello@<your-domain>
```

### 1.4 Check it

- [ ] Admin → Email previews: send yourself each template
- [ ] [mail-tester.com](https://www.mail-tester.com): send a preview to the address it gives you. Aim for 9/10 or better
- [ ] Gmail → open the message → **Show original**: SPF, DKIM and DMARC all say `PASS`
- [ ] The unsubscribe link in a marketing email works and goes to your own site, not a Brevo URL

**The 300-a-day limit:** it resets every day and doesn't roll over. Keep `FLOWS_ENABLED=false` until the order emails work, then switch flows on one at a time under Admin → Email flows.

---

## 2. Monitoring: Better Stack and Sentry

### 2.1 What gets watched

| Monitor | Type | Target | Catches |
|---|---|---|---|
| Storefront | Uptime | `https://<your-domain>/` | Vercel or DNS down |
| API | Uptime | `…/guiltFreeCookie/actuator/health` | App or database down |
| Mail and delivery | Uptime | `…/guiltFreeCookie/actuator/health/dependencies` | Brevo or ShipLogic unreachable |
| Reservation release job | Heartbeat | pinged by the app every run | Scheduler stopped (app asleep, crashed, stuck) |
| Daily email cron | Heartbeat | pinged by the app once a day | Daily flows didn't run |
| Errors | Sentry | Spring Boot + Vue | Exceptions, failed checkouts, broken pages |

The health endpoints come from section 7 of [HOSTING-PLAN.md](HOSTING-PLAN.md).

### 2.2 Better Stack Uptime

1. Sign up at [betterstack.com](https://betterstack.com/uptime) on the free plan.
2. Create the three **Uptime** monitors from the table: HTTP status, 3-minute interval, alert on anything other than 2xx.
3. Create two **Heartbeats**:
   - `reservation-release`: expected every run of `app.order.expiry-check-ms`, with a grace period of 2 runs
   - `daily-flows`: expected every 24 hours, with a 1-hour grace period
4. Copy each heartbeat's URL into App Service settings:

```
HEARTBEAT_RESERVATIONS_URL=https://uptime.betterstack.com/api/v1/heartbeat/<token>
HEARTBEAT_DAILY_FLOWS_URL=https://uptime.betterstack.com/api/v1/heartbeat/<token>
```

5. **Status pages → Create:** add all five monitors, and link the page in your project write-up.
6. **On-call → your profile:** set up email and push alerts with the Better Stack mobile app.

### 2.3 Heartbeat code

One small class. It does nothing when the URL is blank, so local development is unaffected.

`src/main/java/za/ac/cput/guiltfreecookie/service/Heartbeat.java`:

```java
package za.ac.cput.guiltfreecookie.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class Heartbeat {

    private static final Logger log = LoggerFactory.getLogger(Heartbeat.class);

    private final RestClient client;

    public Heartbeat() {
        var timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(3000);
        timeouts.setReadTimeout(3000);
        this.client = RestClient.builder().requestFactory(timeouts).build();
    }

    public void ping(String url) {
        if (url == null || url.isBlank()) return;
        try {
            client.get().uri(url).retrieve().toBodilessEntity();
        } catch (RuntimeException e) {
            log.warn("Heartbeat ping failed: {}", e.getMessage());
        }
    }
}
```

```properties
app.heartbeat.reservations-url=${HEARTBEAT_RESERVATIONS_URL:}
app.heartbeat.daily-flows-url=${HEARTBEAT_DAILY_FLOWS_URL:}
```

Call it as the **last line** of each job, so a ping means the run finished:

- [ReservationReleaseJob.java](../src/main/java/za/ac/cput/guiltfreecookie/service/order/ReservationReleaseJob.java), at the end of the `@Scheduled` method: `heartbeat.ping(reservationsUrl);`
- [FlowTriggers.java](../src/main/java/za/ac/cput/guiltfreecookie/service/flow/FlowTriggers.java), at the end of the `daily-cron` method: `heartbeat.ping(dailyFlowsUrl);`

A failed ping only logs a warning, so a Better Stack outage can't break a job.

### 2.4 Sentry

1. Sign up at [sentry.io](https://sentry.io) on the free Developer plan. Create two projects: **Spring Boot** and **Vue**.
2. **Backend:** add Sentry's Spring Boot starter. Use the artifact Sentry's Spring Boot docs list for **Spring Boot 4**, which is different from the Boot 3 starter. Then set:

```
SENTRY_DSN=<backend project DSN>
SENTRY_ENVIRONMENT=production
SENTRY_SEND_DEFAULT_PII=false
```

3. **Frontend:** `npm install @sentry/vue`, then in `main.js`:

```js
import * as Sentry from "@sentry/vue";

if (import.meta.env.VITE_SENTRY_DSN) {
  Sentry.init({
    app,
    dsn: import.meta.env.VITE_SENTRY_DSN,
    environment: "production",
    sendDefaultPii: false,
  });
}
```

Add `VITE_SENTRY_DSN` to Vercel's environment variables.

4. **Content-Security-Policy:** the page's CSP only allows `connect-src 'self'` plus the API, so the browser would block Sentry's reports. In [vite.config.js](../frontend/vite.config.js), add the ingest origin from your DSN (e.g. `https://o123456.ingest.de.sentry.io`) to `connect-src` when `VITE_SENTRY_DSN` is set.
5. **Privacy:** keep `sendDefaultPii` off. Customer names, emails and addresses must not go to Sentry (POPIA).

### 2.5 Check it

- [ ] All Better Stack monitors green, and both heartbeats received within one cycle
- [ ] Stop the App Service: the API monitor and the reservation heartbeat both alert. Start it again
- [ ] Set a wrong `MAIL_PASSWORD`: the mail and delivery monitor alerts, and the shop keeps running
- [ ] Throw a test exception from a temporary endpoint and from the browser console: both show up in Sentry

---

## 3. Delivery: ShipLogic (The Courier Guy)

### 3.1 What changes for the shop

| Today | After |
|---|---|
| Courier rate is a flat amount set by an admin | Live courier quote from The Courier Guy, based on the delivery address |
| Admin books the courier outside the shop and types in the tracking number | Admin clicks **Book courier**: the shop creates the shipment, stores the tracking number and opens the waybill PDF |
| Customer's tracking only changes when an admin updates it | ShipLogic webhooks move the order to **Shipped** and **Delivered** automatically |
| PUDO and pickup on flat rates | Unchanged for now (see open questions) |

### 3.2 Account and sandbox

1. Ask The Courier Guy for **ShipLogic sandbox API credentials** through [thecourierguy.co.za/integrations](https://thecourierguy.co.za/integrations). Their integrations page says you can start testing on the sandbox straight away.
2. Read the [ShipLogic API docs](https://www.shiplogic.com/api-docs). Confirm the base URL, the auth header, and the exact request fields for rates, shipments, labels, tracking and webhooks before writing the client. Some third-party SDKs say the API token alone decides sandbox or live, on one host. Confirm that too.
3. Known sandbox quirk: rates can work while creating a shipment fails with "insufficient funds". That's the sandbox account's balance, not your code.

```
DELIVERY_API_BASE_URL=<ShipLogic API base URL>
DELIVERY_API_KEY=<sandbox API key>
DELIVERY_WEBHOOK_SECRET=<openssl rand -hex 32>
DELIVERY_COLLECTION_ADDRESS=<bakery street, suburb, city, postal code>
DELIVERY_COLLECTION_CONTACT=<name, phone, email>
```

`DELIVERY_API_BASE_URL` also switches on the `delivery` health check from [HOSTING-PLAN.md](HOSTING-PLAN.md) section 7.3.

### 3.3 Parcel size

ShipLogic prices by weight and dimensions, and returns no rates if they're missing. Cookie packs have no weight yet. Keep it simple with box sizes on **Admin → Delivery**:

| Field | Example |
|---|---|
| Small box (up to N cookies): L × W × H cm, kg | 1–12 cookies: 25 × 20 × 8, 0.8 kg |
| Large box: L × W × H cm, kg | 13+ cookies: 35 × 25 × 10, 1.6 kg |

The shop picks the box from the cart's total cookie count. These are new columns on `ShippingSettings`, so add the SQL to `API-CHANGELOG.md` as usual.

### 3.4 Code structure

```
service/shipping/
  ShippingRateProvider.java       (existing interface)
  RuleBasedShippingProvider.java  (existing: PUDO, pickup, and the courier fallback)
  ShipLogicClient.java            new: HTTP calls only (rates, create shipment, label, tracking)
  ShipLogicShippingProvider.java  new: implements ShippingRateProvider
  ShipmentService.java            new: books a shipment for a paid order
controller/
  DeliveryWebhookController.java  new: POST /api/v1/delivery/webhook
```

**Rates at checkout (`ShipLogicShippingProvider`).**
- Courier: ask `ShipLogicClient` for rates from the collection address to the customer's address, using the chosen box. Show the cheapest standard service, with its delivery estimate.
- PUDO and pickup: take these quotes from `RuleBasedShippingProvider`, unchanged.
- Free delivery (over the threshold, or with a free-shipping code): still R0 to the customer. The shop pays the courier.
- Fallback: if ShipLogic errors or takes longer than 3 seconds, use the admin's flat courier rate, so checkout never breaks because a courier API is down.
- `quotes(…)` currently takes only the total. The courier quote also needs the delivery address and cookie count, so widen the interface to take a small `ShippingRequest` record.
- Cache identical quotes (same postal code and box) for 10 minutes to save API calls.

**The price is locked at checkout.** Store the quoted amount on the order, exactly as the flat rate is stored today. A later quote change must never change what the customer paid.

**Booking (`ShipmentService`).**
- Admin → Orders → a paid courier order → **Book courier** creates the shipment with the service the customer was quoted.
- Save `courierName = "The Courier Guy"`, `trackingNumber`, and `trackingUrl` (the courier's public tracking page) on the order.
- Move the order to **Shipped**, which sends the existing shipped email (`AdminOrderService` already handles this).
- Offer **Download waybill** for the label PDF.
- Book only once per order: if a tracking number exists, show it instead of the button.

**Tracking webhooks (`DeliveryWebhookController`).**
- Register `https://<app>.azurewebsites.net/guiltFreeCookie/api/v1/delivery/webhook?key=<DELIVERY_WEBHOOK_SECRET>` in the ShipLogic dashboard for tracking events. If ShipLogic signs webhooks, verify the signature instead of the `key` parameter.
- Permit `POST /api/v1/delivery/webhook` in `SecurityConfig`, and compare the secret in constant time.
- Find the order by tracking number. Map courier statuses:
  - collected / in transit → **Shipped** (if not already)
  - delivered → **Delivered**
  - failed delivery / returned → add an order event and email the admin. Don't change the status automatically
- Webhooks can arrive twice or out of order: ignore any event that would move an order backwards.
- Always answer 200 once the secret checks out, even for unknown tracking numbers, so ShipLogic doesn't retry forever.

**Delivery-day rule.** Cookies shouldn't sit in a depot over a weekend. Only book Monday to Wednesday (Africa/Johannesburg). On other days, **Book courier** asks for confirmation.

### 3.5 Tests

- `ShipLogicClient`: Spring's `MockRestServiceServer` with recorded sandbox responses (one success, one error, one timeout)
- `ShipLogicShippingProvider`: falls back to the flat rate when the client fails or times out; free delivery still shows R0
- Webhook: wrong secret → 401; duplicate event → no second status change; out-of-order event → ignored; unknown tracking number → 200 and nothing changes
- Booking twice → second call refused

### 3.6 Check it on the sandbox

- [ ] A Cape Town address and a Johannesburg address get different courier prices at checkout
- [ ] With a wrong `DELIVERY_API_KEY`, checkout still works on the flat rate, and the mail and delivery monitor alerts
- [ ] Book courier on a test order: tracking number saved, shipped email sent, waybill downloads
- [ ] Use the sandbox to move a shipment to delivered, or post a recorded event to the webhook: the order turns **Delivered**

### 3.7 Open questions

- **PUDO lockers:** The Courier Guy runs PUDO. Check whether ShipLogic can quote and book locker deliveries. If it can, PUDO moves onto the same client; if not, it stays on the flat rate.
- **Going live:** a live ShipLogic account needs a business account with The Courier Guy and prepaid credit. For the demo, the sandbox is enough.

---

## Sources

Checked 3 November 2026.

- [The Courier Guy integrations](https://thecourierguy.co.za/integrations): ShipLogic API and sandbox
- [ShipLogic API docs](https://www.shiplogic.com/api-docs)
- [Starshipit: setting up The Courier Guy](https://support.starshipit.com/articles/90200000000108-how-to-set-up-the-courier-guy): the sandbox "insufficient funds" quirk
- [shiplogic-php-sdk](https://packagist.org/packages/sonnenglas/shiplogic-php-sdk) (unofficial): one host, token decides sandbox or live; webhook topics
- [Bob Go open API](https://bobgo.co.za/?p=21315): docs only through sales
- [Brevo free SMTP](https://www.brevo.com/free-smtp-server): 300 emails a day
- [Better Stack vs Healthchecks.io](https://betterstack.com/community/comparisons/better-stack-vs-healthchecks-io/) and [Better Stack pricing review](https://cubeapm.com/blog/betterstack-pricing-review/): free tier limits
- [UptimeRobot free-plan terms](https://dev.to/velprove/uptimerobot-commercial-use-free-alternatives-for-business-sites-in-2026-5d75): commercial-use changes
