# What this stack costs a month

What Guilt Free Cookie costs to run **without the student account**, using a free plan wherever one exists, and what to charge a client for it. Prices checked **7 November 2026**. Rand amounts use **R18 to the US dollar**; update that rate before quoting.

## Summary

Only two things can't be free: the **Azure server** for the API and, after the first year, the **database**. Everything else runs on free plans.

| Period | US$ / month | R / month (at R18) |
|---|---|---|
| **Year 1** (new Azure account: MySQL free for 12 months) | **≈ $19.34** | **≈ R350** |
| **From year 2** | **≈ $39.87** | **≈ R720** |
| From year 2 with the bigger B2 server (optional) | ≈ $57.68 | ≈ R1,040 |

| | Year 1 | From year 2 |
|---|---|---|
| Azure App Service B1 (API, staging and production on one server) | $17.74 | $17.74 |
| Azure MySQL B1ms + 32 GB storage | $0 | $20.53 |
| Domain (≈ $19/year) | $1.60 | $1.60 |
| Vercel, Sentry, Better Stack, Grafana, Brevo, ImprovMX, Google Places, GitHub | $0 | $0 |
| **Total** | **$19.34** | **$39.87** |

On top of that, every sale pays **PayFast's fee** (about 3.2% + R2 on cards, plus VAT). The **courier** is paid by the customer at checkout, except on free-delivery orders. Both are covered in "Per-order costs".

**Upgrade MySQL 8.0 to 8.4 first.** The database runs MySQL 8.0, which is now on Azure's paid **extended support: $0.10 per vCore-hour, about $73/month**, more than the database itself. It's $0 today only because of the student free tier, and the totals above assume it's gone. Upgrade in the Azure portal: `gfc-mysql` → **Upgrade**.

## Service by service

### Azure: the API and the database (paid)

Prices from Azure's public price list for South Africa North, pay-as-you-go, 730 hours a month.

| Item | Size | Rate | Per month | Free option? |
|---|---|---|---|---|
| **App Service plan (Linux)** | **B1**: 1 core, 1.75 GB | $0.0243/hour | **$17.74** | **No.** The free F1 plan allows 60 CPU minutes a day and sleeps when idle; the Java API needs 4–5 minutes of CPU just to start. B1 is the cheapest plan that runs it |
| | B2: 2 cores, 3.5 GB (optional) | $0.0487/hour | $35.55 | Twice the memory (B1 runs at 85–90%) and faster starts |
| **MySQL compute** | B1ms: 1 core, 2 GB | $0.0215/hour | **$15.70** | **Free for 12 months** on a new Azure account (750 hours a month of B1ms) |
| **MySQL storage** | 32 GB | $0.151/GB | **$4.83** | **Free for 12 months** (32 GB) on a new Azure account |
| MySQL backups | 7 days | — | $0 | Free up to the storage size |
| MySQL 8.0 extended support | 1 vCore | $0.10/hour | $73.00 | Avoided by upgrading to 8.4 |
| Data out | — | — | $0 | First 100 GB a month free |

A new Azure account also comes with $200 of credit for the first 30 days, which covers the first month's App Service.

What's billed today on the student account (1–7 November 2026): $1.95 for B1; MySQL, storage and data out $0.

### Free services

| Service | Used for | Free plan covers | This shop uses | Paid plan, if outgrown |
|---|---|---|---|---|
| **Vercel** Hobby | The storefront | Hosting, HTTPS, custom domain, builds on every push | Well inside it | Pro $20/month. Vercel's terms say Hobby is for non-commercial projects, so a client earning from the shop may be asked to move to Pro |
| **Sentry** Developer | Error reports | 1 user, 5,000 errors a month | One person, a few errors | Team ≈ $29/month: more users, 50,000 errors |
| **Better Stack** | Uptime, heartbeats, status page | 10 monitors and heartbeats, 1 status page, email alerts | 5 checks and the status page | ≈ $29–34/month for phone/SMS alerts and an unbranded status page |
| **Grafana Cloud** | Dashboard, synthetic checks | 10,000 metric series, 14 days kept, 100,000 check runs a month | A few hundred series, about 60,000 runs (planned) | Pro from ≈ $19/month plus usage |
| **Brevo** | Sending email | 300 emails a day (about 9,000 a month) | Order emails | Starter ≈ $9/month: no daily cap. Needed once marketing emails could pass 300 in a day |
| **ImprovMX** | Receiving `hello@` | 1 domain, 25 aliases, 500 forwarded a day | 1 alias | Premium ≈ $9/month |
| **Google Places** | Address search | 10,000 autocomplete requests a month, then $2.83 per 1,000 | About 5–10 requests per checkout, so roughly 1,000–2,000 checkouts a month free | Pay-as-you-go above the cap |
| **GitHub** | Code, Actions | 2,000 Actions minutes a month | About 10 minutes per release | — |
| **ShipLogic** | Courier API | No API fee | — | Shipments are paid per parcel (below) |

### Domain (paid, small)

| | Cost | Notes |
|---|---|---|
| `guiltfreecookie.me` | ≈ $19.18/year (≈ $1.60/month) | Free for the first year only through the GitHub Student Pack. Without it, this is the price from day one |
| Alternative: a `.co.za` | ≈ R100/year | Cheaper and local; would need a new domain name and moving the DNS |

## Per-order costs

These grow with sales, not with the month.

| What | Cost | Example: R500 order |
|---|---|---|
| **PayFast, card or Apple/Google Pay** | 3.2% + R2.00, plus 15% VAT | R16.00 + R2.00 = R18.00, + VAT = **R20.70** |
| **PayFast, Instant EFT or Capitec Pay** | 2.0% (minimum R2.00), plus VAT | R10.00 + VAT = **R11.50** |
| **Courier (The Courier Guy)** | Per parcel, by area and size (sandbox quote to Johannesburg: R95) | **Paid by the customer** at checkout. On free delivery (from R750) the shop pays: budget about R95–R150 a parcel |

PayFast has no monthly fee. Its card rate fell from 3.5% to 3.2% on 1 June 2026; confirm on PayFast's fees page before quoting.

## What to charge a client

The bills above are only what the services charge. A client also pays for your time: updates, security fixes, watching alerts, and fixing what breaks. Full packages and prices are in [PACKAGES.md](PACKAGES.md).

For a live client, add **Vercel Pro ($20/month)**: Vercel's Hobby plan doesn't allow business use.

**1. Hosting at cost plus a margin** (about 25%, for exchange-rate swings and account admin):

| Period | Your cost | Charge (+25%) |
|---|---|---|
| Year 1 | ≈ R350 | ≈ **R440** |
| From year 2 | ≈ R720 | ≈ **R900** |
| From year 2 with B2 | ≈ R1,040 | ≈ **R1,300** |

Quoting the year-2 price from the start keeps the client's bill steady and covers the jump when the free MySQL year ends.

**2. A monthly maintenance retainer**, for example:

| Package | Includes | Price |
|---|---|---|
| Basic | Hosting, monitoring, security updates, up to 2 hours of fixes | Hosting + 2 × your hourly rate |
| Standard | Basic, plus up to 5 hours of changes and a monthly report from the Grafana dashboard | Hosting + 5 × your hourly rate |

**3. Pass the per-order costs on as they are.** PayFast pays the client minus its fee, and the courier is on the customer's bill, so neither goes through you.

**Make the client the owner** of the Azure subscription, Vercel account, domain, PayFast and Brevo, with you added as an admin. Their card is billed directly; you invoice your margin and time. If they leave, nothing has to move.

## Before handing it to a client

- [ ] Upgrade MySQL 8.0 → 8.4 (avoids ≈ $73/month of extended support)
- [ ] New pay-as-you-go Azure account in the client's name (gets the 12-month MySQL free tier), and move the App Service and database there
- [ ] Set an Azure budget alert at the expected total (today: `gfc-monthly-15`, $15)
- [ ] Live PayFast and ShipLogic keys, real collection address and box sizes (TODO section 4)
- [ ] Remove the `showcase` profile (TODO section 5)

## Sources

- Azure: [Azure retail prices API](https://prices.azure.com/api/retail/prices), South Africa North, read 7 November 2026; billed amounts from Azure Cost Management
- Vercel: [vercel.com/pricing](https://vercel.com/pricing); [stork.ai](https://www.stork.ai/blog/vercel-pricing-2026), [temps.sh](https://temps.sh/blog/vercel-pricing-complete-guide-2026)
- PayFast: [growthpulsemedia.co.za](https://www.growthpulsemedia.co.za/payfast-vs-ozow/), [tendaigumunyu.co.za](https://tendaigumunyu.co.za/journal/payment-gateways-south-africa-2026) (third-party; confirm on PayFast's fees page)
- Google Places: [Google Maps Platform pricing](https://developers.google.com/maps/billing-and-pricing/pricing), [Places API usage and billing](https://developers.google.com/maps/documentation/places/web-service/usage-and-billing)
- Brevo: [emailtooltester.com](https://www.emailtooltester.com/en/reviews/brevo/pricing/), [omnisend.com](https://www.omnisend.com/blog/brevo-pricing/)
- Better Stack: [pagerly.io](https://www.pagerly.io/blog/better-stack-pricing), [vantaj.co](https://vantaj.co/blog/better-stack-pricing-2026/)
- Sentry: [activepieces.com](https://www.activepieces.com/blog/sentry-pricing-2026-developer-team-business-plans), [costbench.com](https://www.costbench.com/software/developer-tools/sentry/)
- Grafana Cloud: [grafana.com/pricing](https://grafana.com/products/cloud/pricing/), [last9.io](https://last9.io/blog/grafana-cloud-pricing/)
- ImprovMX: [improvmx.com/pricing](https://www.improvmx.com/pricing)
- Domain: [domainoffer.net](https://domainoffer.net/tld/me/namecheap)

Third-party prices can lag behind the providers' own pages. Check each provider's pricing page before sending a quote. The 12-month MySQL free tier (750 hours of B1ms and 32 GB) is part of Azure's free account offer; confirm it's still offered when the client's account is created.
