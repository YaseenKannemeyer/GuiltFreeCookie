# Handoff: Phase 6 + frontend for Phases 1–2 (ChatGPT)

You're the second engineer on GuiltFreeCookie. Claude is building Phases 3–5 (cart → checkout/orders/PayFast → account order history) on branch `Completing-the-site` at the same time. Your scope is listed below; it has no dependency on Claude's in-flight work.

**Work on a new branch** `phase-6-chatgpt`, created from `Completing-the-site` after the Phase 2 commit. Keep your work in new files and packages wherever possible; see "Shared files" for the rules.

Read first: `docs/API-CHANGELOG.md` (the API contract so far) and Swagger UI at `http://localhost:8080/guiltFreeCookie/swagger-ui.html`.

---

## Stack and conventions (follow these exactly)

- Spring Boot 4.1, Java 25, Jackson 3, MySQL (H2 in tests). Vue 3 + Vite + Tailwind frontend in `frontend/`.
- **Entities** use a hand-written `Builder` with `copy()`, a protected no-arg constructor and getters only; there are no setters. See `domain/CookieVariant.java`. Entities refer to each other by plain id columns (`cookieId`, `customerEmail`), not JPA relations.
- **Validation** goes through `util/Helper` (`clean`, `cleanEmail`, `isValidEmail`, `isPresent(value, max)`, `isWithin`, `isValidMobileNumber`). New code throws `exception/BadRequestException`, `NotFoundException`, `ConflictException` or `UnauthorizedException`; `config/GlobalExceptionHandler` turns them into `{timestamp, status, error, message, fieldErrors}`.
- **Request DTOs are records with boxed types only** (`Boolean`, `Integer`). Jackson 3 rejects a missing primitive with a 400.
- **Security** (`config/SecurityConfig`): every route requires ADMIN unless it is listed as `permitAll`. Customer routes are under `/api/v1/me/**` (ROLE_CUSTOMER). In a controller, `@AuthenticationPrincipal String` is the admin id or the customer email.
- **Rate limits** live in `security/RateLimitFilter`; add a rule for any new public POST.
- **Email** goes through `service/notification/EmailService` (Spring Mail, `@Async`). With `MAIL_ENABLED=false` it logs instead of sending. Test locally with Mailpit (`brew install mailpit`, SMTP 1025, UI at http://localhost:8025, `MAIL_ENABLED=true`).
- **Tests:** Mockito unit tests in the existing style (see `service/CustomerAuthServiceTest`), `@DataJpaTest` for real queries (see `repository/CookieRepositoryTest`), and route rules in `security/SecurityConfigTest`. `./mvnw test` must pass.
- **Comments:** few. Only where something is genuinely non-obvious.
- **Every change ships with:** Swagger `@Tag`/`@Operation` annotations and a dated `docs/API-CHANGELOG.md` entry (newest first).

## Shared files (Claude edits these too, so keep your edits small and additive)

`config/SecurityConfig.java` (add your `permitAll` lines only), `security/RateLimitFilter.java` (add rules only), `src/main/resources/application*.properties` and `src/test/resources/application.properties` (append a section), `pom.xml` (add dependencies only), and `service/catalog/CatalogService.java` (see D). Don't touch `Cart*`, anything order, payment or shipping, or `CustomerAuthService`.

---

## A. Email design system (backend)

Every email must look like the website. Tokens come from `frontend/tailwind.config.js`:

- **Colours:** chocolate `#2E0E0B` (text, header and footer bands), caramel `#C59F71` (buttons; use `#9A6E45` for links on cream), tan `#D0AB7B` (hero blocks), cream `#E3D9C8` (page), `#F5F0E6` (cards).
- **Fonts:** Luckiest Guy (headlines) and Nunito (body), loaded from Google Fonts in `<head>`, with fallbacks `'Arial Black', Arial` and `'Trebuchet MS', Arial, sans-serif`.
- **Wordmark:** "GuiltFree" in chocolate and "Cookie" in caramel, set in Luckiest Guy. Render it as a retina PNG (`src/main/resources/static/email/wordmark@2x.png`) so it's identical everywhere; Gmail and Outlook strip web fonts.

Build:
1. Add `spring-boot-starter-thymeleaf`. Templates go in `templates/email/`, fragments in `templates/email/fragments/`. Components:
   - header band with the wordmark;
   - tan hero with a big headline and a cookie image;
   - **bulletproof pill CTA** (rounded-full caramel button with chocolate text, plus a VML fallback for Outlook);
   - product card (cookie on a tinted 4:5 tile, name, "from" price, link) and a 2-up product grid;
   - order summary table;
   - discount "ticket" (dashed caramel border, code in Luckiest Guy, expiry date);
   - review quote with stars;
   - founder note;
   - footer (chocolate band, social links, business address, "why you got this", unsubscribe link).
2. Layout rules: 600px single column, table-based, **inline styles** (put the tokens in fragments; don't use a CSS inliner), fluid below 600px, buttons at least 44px tall, `alt` and width/height on every image, `color-scheme: light` meta, a preheader, and under 100 KB of HTML.
3. `EmailService`: add `sendTemplate(to, subject, template, model, marketing)` that sends **multipart/alternative** (HTML plus a plain-text version) and, when `marketing` is true, a `List-Unsubscribe` header. Keep the existing `send(...)` working.
4. Absolute image URLs: add `app.public-base-url` (`${PUBLIC_BASE_URL:http://localhost:8080/guiltFreeCookie}`).
5. **Email-safe product images:** product photos are transparent WebP, which Outlook can't show. Add the TwelveMonkeys `imageio-webp` dependency and a public `GET /api/v1/email-assets/cookies/{cookieId}.png`. It composites the cookie's primary image onto a cream tile with a soft chocolate drop shadow, at about 600px, caches the result under `uploads/email/`, and serves it with long cache headers. Return 404 for unknown or archived cookies.
6. Move the password-reset email (`CustomerAuthService.forgotPassword`) onto a branded template. This is the only line you may change in that class.
7. Templates to design now, with sample data: `password-reset`, `contact-auto-reply`, `contact-to-shop`, plus the transactional set Claude will wire in Phase 4: `order-confirmation` (thank-you from the founder, items, totals, delivery or pickup details, tracking link), `order-shipped` (courier and tracking button), `order-ready-for-pickup`. Model shapes for the order templates are up to you; document them in the changelog and Claude will fill them.
8. Admin previews:
   - `GET /api/v1/admin/email-previews` lists template names;
   - `GET /api/v1/admin/email-previews/{template}` returns rendered HTML with sample data;
   - `POST /api/v1/admin/email-previews/{template}/send-test` sends it to the logged-in admin's email.

Config for the footer and founder note, under `app.site.*` (see C): founder name, founder message, founder photo URL, social links, business address.

## B. Contact form (backend)

- `ContactMessage` entity: id, name, email, phone (optional), subject, message, createdAt, status `NEW|RESOLVED`, resolvedAt, resolvedBy.
- `POST /api/v1/contact` is public: `{name, email, phone?, subject, message, website}`. `website` is a honeypot: if it's filled, return 204 and drop the message. Validate lengths. Add a rate limit (5 per hour per IP). Save the message, email the shop inbox (`app.site.support-email`) and send the customer an auto-reply that states the expected response time.
- Admin endpoints:
  - `GET /api/v1/admin/messages?status=` (newest first);
  - `PUT /api/v1/admin/messages/{id}/resolve`.

## C. Site info (backend)

`GET /api/v1/site/info` is public and feeds the homepage trust elements, the contact page and the footer:
```json
{ "supportEmail": "...", "phone": "...", "whatsapp": "...", "hours": "Mon–Fri 08:00–17:00",
  "responseTime": "We reply within 1 business day", "address": "...",
  "social": { "instagram": "...", "facebook": "...", "tiktok": "..." },
  "stats": { "averageRating": 4.8, "reviewCount": 132, "customersServed": null, "cookiesSold": null } }
```
Contact fields come from `app.site.*` properties (env-overridable, with sensible placeholders). The rating stats come from approved reviews (D). `customersServed` and `cookiesSold` stay `null` until Claude adds orders in Phase 4.

## D. Reviews (backend)

- `Review` entity: id, cookieId, rating 1–5, title (≤100), body (≤2000), authorName, customerEmail, verifiedBuyer, status `PENDING|APPROVED|REJECTED`, createdAt, moderatedAt.
- Endpoints:
  - `POST /api/v1/me/reviews/{cookieId}` needs a customer token. One review per customer per cookie (409 on a second). It starts as PENDING.
  - `GET /api/v1/cookies/{idOrSlug}/reviews?page=&size=&sort=newest|highest|lowest` is public and returns approved reviews only, as `{summary: {averageRating, reviewCount, histogram: {5: n, …, 1: n}}, page: PageResponse<ReviewView>}`. Never expose the customer's email.
  - `GET /api/v1/reviews/featured?limit=6` is public: approved reviews rated 4 or 5 with the longest bodies, for the homepage testimonials.
  - Admin moderation: `GET /api/v1/admin/reviews?status=` and `PUT /api/v1/admin/reviews/{id}/approve|reject`.
- **Verified buyer:** put the check behind an interface `PurchaseVerifier { boolean hasBought(String email, String cookieId); }` whose only implementation for now returns `false`. Claude replaces it in Phase 4.
- **Wire ratings into the catalog:** in `CatalogService`, `CookieSummary` and `CookieDetail` currently pass `null, 0` for `averageRating` and `reviewCount`. Replace those two spots using a batch lookup (one query per page, not one per cookie). Don't change anything else in that file.
- Note: `GET /api/v1/cookies/**` is already public, so add your reviews path to that matcher's tests.

## E. Frontend

The Phase 1 and 2 API contracts are in the changelog. Build or upgrade, mobile-first, in the existing style:
- **Auth and account (Phase 1):** Login, Register (with newsletter opt-in), Forgot password, Reset password (reads `?token=`), My account (profile and change password). Store the customer token the way `useAdminAuth` stores the admin session (as a separate store). Attach it in an axios interceptor for `/api/v1/me/**`, and on a 401 clear it and send the user to login.
- **Catalog (Phase 2):**
  - Product list and search: debounced `q`, category chips from `/cookies/categories`, a price filter, an in-stock toggle, sort, and pagination or infinite scroll.
  - Product detail by slug (`/product/:slug`) with gallery, pack picker (disable unavailable packs), price per cookie, a low-stock badge, a nutrition panel and related cookies.
  - Homepage featured row (`?featured=true`).
  - Keep `/product/:id` links working by redirecting to the slug.
- **Admin catalog (Phase 2):**
  - Stock adjust and stocktake, with a history drawer.
  - Low-stock dashboard widget.
  - Pack-size editor.
  - Gallery upload and reorder.
  - Details form (slug, featured, nutrition).
- **Phase 6 pages:**
  - Contact page wired to `/contact` and `/site/info`.
  - Reviews on product detail: histogram, list and a write-a-review form for logged-in customers.
  - Homepage testimonials and trust stats from `/site/info`.
  - Admin inbox, admin review moderation, and admin email previews (render the HTML in a sandboxed iframe).
- Cart and checkout pages are **not** in scope yet: their API arrives with Phase 3–4.

## Definition of done

`./mvnw test` passes; there are tests for every new service and route rule; Swagger shows the new endpoints; `docs/API-CHANGELOG.md` has a "Phase 6" entry; every template renders in the admin preview and in Mailpit; and the frontend builds (`npm run build`) and works at phone width.
