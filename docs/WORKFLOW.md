# How a change gets to production

This guide follows one change from your laptop to `guiltfreecookie.me`. It covers the branches and their rules, the tests, the GitHub Actions, how Azure (API) and Vercel (storefront) get the new code, and how the site is watched once it's live.

## The big picture

```
your laptop ──push──▶ feature branch ──PR──▶ staging ──PR──▶ main
                                                │              │
                          GitHub Actions: tests + deploy       │  GitHub Actions: tests only
                                                ▼              ▼
                          Azure  guiltfreecookie-api-staging   Azure  guiltfreecookie-api   ◀── deployed by hand (step 7)
                          Vercel staging preview               Vercel guiltfreecookie.me    ◀── deploys by itself
```

| | Staging | Production |
|---|---|---|
| Branch | `staging` | `main` |
| Storefront (Vercel) | the `staging` branch preview URL | [guiltfreecookie.me](https://guiltfreecookie.me) |
| API (Azure App Service) | `guiltfreecookie-api-staging` | `guiltfreecookie-api` |
| Database (Azure MySQL `gfc-mysql`) | `guiltfreecookie_staging` | `guiltfreecookie` |
| API deploys | automatically on every push to `staging` | only when someone runs the workflow by hand |
| Storefront deploys | automatically on every push to `staging` | automatically on every push to `main` |

Both run the `prod,showcase` profiles: PayFast and ShipLogic sandboxes, so no real money moves and no real courier is booked.

## Branch protection: what it is and what's set

Branch protection is a set of GitHub rules that stop anyone changing an important branch carelessly, for example by pushing straight to it or rewriting its history.

**`main`** has a ruleset called `protect-main`:

| Rule | What it means |
|---|---|
| Pull request required | Changes reach `main` only through a merged pull request, never by pushing to it directly |
| 1 approval required | Someone other than the author must approve the pull request |
| Stale approvals dismissed | A new push to the pull request cancels earlier approvals, so the final version is what gets approved |
| Last push must be approved | Whoever pushed last can't be the approver |
| No force push | `main`'s history can't be rewritten |
| No deletion | `main` can't be deleted |
| Bypass: repository admins | You, as the repo admin, can skip these rules (GitHub shows a "bypass" checkbox on the merge button). Use it only when you're working alone and staging is already checked |

**`staging`** has no rules: you can push to it directly. Pull requests into `staging` are still a good habit, because they run the tests before the merge.

Neither branch requires the tests to pass before merging. GitHub shows the result on the pull request, but it won't block a merge with failing tests, so check it yourself. (To make it block: **Settings → Rules → protect-main → Require status checks to pass**, add `verify`.)

## Step by step

### 1. Start from the latest staging

```sh
git checkout staging
git pull
git checkout -b feature/short-name
```

All work starts from `staging`, never from `main`.

### 2. Make the change and test it locally

Run the whole shop on your laptop with demo data (needs Java 25 and Node 22.12+):

```sh
cd frontend && npm ci && cd ..
bash scripts/run-demo.sh
```

- Storefront: http://127.0.0.1:5173
- Admin: http://127.0.0.1:5173/admin (`owner@example.com` / `DemoCookies123!`, local only)
- Emails appear under **Admin → Email previews → Demo inbox**

The demo uses its own throwaway database (H2 under `target/demo/`) and never touches Azure.

Run the same tests GitHub will run:

```sh
./mvnw verify                 # backend: all tests, then builds the jar
cd frontend
npm test                      # frontend unit tests
npm run build                 # frontend build, including the prerendered pages
npm run test:responsive       # optional: phone and desktop layout checks in a browser
```

If these pass on your laptop, they'll almost always pass on GitHub.

### 3. Commit and push your branch

```sh
git add <files>
git commit -m "feat: what changed"
git push -u origin feature/short-name
```

Pushing a feature branch on its own deploys nothing.

### 4. Pull request into `staging`

Open a pull request from `feature/short-name` into `staging` (on GitHub, or `gh pr create --base staging`).

GitHub Actions runs the tests for whatever you changed:
- Backend files changed → **Verify and deploy API** (only the `verify` job)
- Frontend files changed → **Verify storefront**

Wait for green ticks, then merge.

### 5. The merge deploys staging by itself

Merging puts the change on `staging`, and that push starts:

| What | Happens |
|---|---|
| **Verify and deploy API** | Tests the backend, builds the jar and deploys it to `guiltfreecookie-api-staging`. Runs only if backend files changed |
| **Verify storefront** | Tests and builds the frontend. Runs only if frontend files changed |
| **Vercel** | Builds the storefront and publishes the staging preview. Runs on every push |

The API takes **about 4–5 minutes to start** after a deploy (it's a small, shared Azure plan). While it starts, the staging storefront can show errors. That's normal.

Check it's back: `https://guiltfreecookie-api-staging.azurewebsites.net/guiltFreeCookie/actuator/health` should say `"status":"UP"`.

### 6. Test on staging

On the staging storefront, try what you changed, then go through the main path:
- Browse, add to cart, check out with a PayFast **sandbox** payment
- Check the order in **Admin → Orders** and that its email arrived (staging's inbox is under **Admin → Email previews**)
- If you changed delivery: address search, courier quote, and **Book courier** on a paid order
- Glance at Sentry (`staging` environment) and the Grafana dashboard (Environment `staging`) for new errors

### 7. Release to production

**a. Merge `staging` into `main`.** Open a pull request from `staging` into `main` (`gh pr create --base main --head staging`), get the approval (or use the admin bypass), and merge.

That push to `main`:
- runs the tests again (**Verify and deploy API** and/or **Verify storefront**), but **does not deploy the API**
- makes **Vercel** rebuild and publish `guiltfreecookie.me` by itself

**b. Deploy the API to production by hand.** Do this before Vercel's build finishes, or right after the merge:

- On GitHub: **Actions → Verify and deploy API → Run workflow**, branch **`main`**, environment **`production`**, **Run workflow**
- Or in the terminal: `gh workflow run deploy-api.yml --ref main -f environment=production`

It tests again and deploys to `guiltfreecookie-api`. If you start it from any branch other than `main`, it stops with "Production deploys must run from main".

**c. Wait until production answers** (about 5 minutes): `https://guiltfreecookie-api.azurewebsites.net/guiltFreeCookie/actuator/health` says `UP`. Then do a quick check on `guiltfreecookie.me`.

**Never restart or deploy staging and production at the same time.** They share one small server, and starting both at once can take both down. Wait for one to answer before touching the other.

### 8. Bring `staging` level with `main` (only if needed)

If something was fixed directly on `main`, merge `main` back into `staging` so the next release doesn't undo it:

```sh
git checkout staging && git pull && git merge origin/main && git push
```

## The GitHub Actions

There are two workflows in `.github/workflows/`. Neither runs on changes it doesn't cover (for example, editing only `docs/` runs nothing).

### Verify and deploy API (`deploy-api.yml`)

Runs when `src/`, `pom.xml`, cookie images or the workflow itself change.

| Trigger | `verify` job | `deploy` job |
|---|---|---|
| Pull request | ✅ | — |
| Push to `staging` | ✅ | ✅ deploys to staging |
| Push to `main` | ✅ | — (production is by hand) |
| Run by hand (**Run workflow**) | ✅ | ✅ to the environment you pick |

- **`verify`**: sets up Java 25, runs `./mvnw -B verify` (every backend test, then the jar build) and keeps the jar for the next job.
- **`deploy`**: picks a GitHub **environment** (`staging` or `production`). Each environment holds two values:
  - `AZURE_WEBAPP_NAME` (a variable): `guiltfreecookie-api-staging` or `guiltfreecookie-api`
  - `AZURE_WEBAPP_PUBLISH_PROFILE` (a secret): the App Service's publish profile, a deploy password downloaded from Azure

  It uploads the jar with `azure/webapps-deploy`. Azure then restarts the app on the new jar.

Two runs for the same environment never deploy at once; the second waits.

### Verify storefront (`frontend.yml`)

Runs when `frontend/` changes, on pull requests and pushes to `main` and `staging`. It runs `npm ci`, `npm test` and `npm run build`. It **doesn't deploy**: Vercel does that on its own (below).

## How the backend reaches Azure

- **Azure App Service** (Linux, Java 25, plan `asp-guiltfreecookie`, B1) runs the jar, one app per environment.
- **Settings** (database password, PayFast, ShipLogic, Google, Brevo, Sentry, Grafana keys) are App Service **environment variables**, never in the repo. They're in the Azure portal under the app → **Settings → Environment variables**, or `az webapp config appsettings list`. **Changing a setting restarts the app.**
- **Database**: Azure MySQL `gfc-mysql`, reachable only from Azure services.
- **Startup**: about 4–5 minutes. Azure allows up to 10 (`WEBSITES_CONTAINER_START_TIME_LIMIT=600`) before it calls the start a failure.

## How the frontend reaches Vercel

- The Vercel project `guilt-free-cookie` is connected to the GitHub repo, so **every push builds the storefront**: pushes to `main` publish `guiltfreecookie.me`, and pushes to `staging` publish the staging preview.
- Vercel runs `npm ci` then `npm run build:demo` in `frontend/` (see `frontend/vercel.json`).
- **Environment variables** in Vercel tell each build which API to use:
  - `VITE_API_BASE_URL`: the API the browser calls (production or staging)
  - `PRERENDER_API_URL`: the API the build reads the catalogue from
  - plus `VITE_SITE_URL`, `VITE_SENTRY_DSN`, `VITE_SENTRY_ENVIRONMENT`, `VITE_DEMO_MODE`, and `VITE_STAGING` on staging
- **Prerendering**: the build fetches the cookie catalogue from the API and saves ready-made product pages. If the API is restarting, the build retries every 20 seconds for up to 10 minutes, which covers the API's startup after a merge. If it still fails, redeploy from the Vercel dashboard once the API answers.
- **DNS**: `guiltfreecookie.me` uses Vercel's nameservers; `www` redirects to the bare domain, and `status` points to Better Stack.

## Monitoring

| Tool | What it watches | How it works | You hear about it by |
|---|---|---|---|
| **Better Stack** monitors | Production: **Storefront** (`guiltfreecookie.me` loads), **API** (`/actuator/health`: running and database reachable), **Dependencies** (`/actuator/health/dependencies`: Brevo email, the email queue, ShipLogic) | Better Stack visits each URL every 3 minutes from Europe and North America. It waits 5 minutes before alerting, so a normal restart doesn't count | Email to `yaseenkannemeyer@gmail.com` |
| **Better Stack** heartbeats | Production's background jobs: **Reservations** (frees stock from unpaid checkouts, every 5 minutes) and **Daily flows** (09:00 marketing emails) | The API pings a secret URL after each run. No ping in time → alert. Daily flows always alerts while `showcase` is on, because that job is off | Email |
| **Status page** | [status.guiltfreecookie.me](https://status.guiltfreecookie.me): the three production monitors, for customers | Built by Better Stack from the monitors above | — |
| **Sentry** | Errors in the API (`guiltfreecookie-api`) and in shoppers' browsers (`guiltfreecookie-web`), on both production and staging | The app sends each crash with where it happened. No personal data | Email on each new kind of error |
| **Grafana Cloud** dashboard | Shop numbers (orders, revenue, emails, courier quotes and bookings, address searches), API health (requests, errors, response time, memory, database pool) and Azure (CPU, memory, restarts, MySQL) | Every minute, each API pushes its numbers to Grafana (`OTLP_METRICS_*` settings). Grafana reads the Azure numbers itself with a read-only Azure login. Pick **production** or **staging** at the top | Nothing alerts yet; open it to look: [broadtarantula971.grafana.net](https://broadtarantula971.grafana.net) |
| **Grafana synthetic checks** (planned) | Both storefronts and APIs, SSL expiry, the email MX record. Production every 5 minutes, staging less often | Grafana's probes visit each URL | Grafana alerts |

**After a release, look at:** the status page (all green), Sentry (no new issues for `production`) and the Grafana dashboard (Environment `production`: requests flowing, no 5xx spike).

## When something goes wrong

| Symptom | Likely cause | What to do |
|---|---|---|
| API health doesn't answer for a few minutes after a deploy | Normal startup (4–5 minutes) | Wait. Don't restart it, which starts the clock again |
| Still down after 10 minutes | The start failed | Azure portal → the app → **Log stream**, or check Sentry |
| Vercel build failed after a merge | The API was restarting longer than the 10-minute retry | Vercel → Deployments → **Redeploy** once the API answers |
| Production deploy says "must run from main" | The workflow was started from another branch | Run it again with branch **main** |
| Better Stack "Daily flows" down | Expected while `showcase` is on | Pause it in Better Stack |
| Tests fail on GitHub but pass locally | Usually Java/Node version, or a test that depends on time or order | Open the run's log on the **Actions** tab |
