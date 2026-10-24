# Phase 6 email integration

All templates live under `src/main/resources/templates/email/`. Render with:

```java
emailService.sendTemplate(recipient, subject, "order-confirmation", model, false);
```

Amounts are **already-formatted display strings** (e.g. `R 240.00`), not a new pricing calculation. Pass absolute HTTP(S) action/image URLs. The HTML escapes model text; do not pass HTML. Use a mutable map when a model contains null values (`Map.of` rejects nulls).

Common optional values:

| Key | Meaning |
|---|---|
| `actionUrl` | Absolute CTA URL. Defaults to `${FRONTEND_BASE_URL}/products`. For orders, pass the guest access-token tracking link or authenticated order page. |
| `actionLabel` | CTA label, default `Visit the bakery`. |
| `heroImage` | Absolute image URL, default `${PUBLIC_BASE_URL}/email/cookie.png`. Use `${PUBLIC_BASE_URL}/api/v1/email-assets/cookies/{cookieId}.png` for a product photo. |
| `unsubscribeUrl` | Required absolute HTTPS URL when `marketing=true`; omitted for transactional messages. |

The renderer supplies `site`, `publicBase`, `frontendBase`, the CTA VML fallback and the `marketing` flag. These are reserved keys; callers should not override them.

## Template-specific models

| Template | Required fields |
|---|---|
| `password-reset` | `actionUrl`, `actionLabel`, `resetMinutes` (number). The existing password-reset service is wired. |
| `contact-auto-reply` | `name`, `subject`, `message`. Response time and founder note come from `app.site.*`. |
| `contact-to-shop` | `name`, `email`, `phone` (empty string if absent), `subject`, `message`, `messageId`. |
| `order-confirmation` | `name`, `orderNumber`, `items`, `subtotal`, `discount`, `shipping`, `total`, `actionUrl`, `actionLabel`; optional nullable `vat`, `deliveryDetails`, `pickupDetails`. Supply the applicable delivery/collection text. |
| `order-shipped` | `name`, `orderNumber`, `courierName`, `trackingNumber`, `deliveryDetails`, `actionUrl`, `actionLabel`. |
| `order-ready-for-pickup` | `name`, `orderNumber`, `pickupDetails`, `actionUrl`, `actionLabel`. |

Each `items` element is a map (or a Java object with equivalent readable properties):

```java
Map.of(
    "name", "Double Chocolate Chunk",
    "variant", "Box of 6",
    "quantity", 1,
    "lineTotal", "R 180.00"
)
```

Order confirmation includes a founder thank-you, item table, totals and delivery/pickup details. Set `vat` only when VAT display applies. `deliveryDetails` and `pickupDetails` are plain strings; newlines are preserved. The shipped CTA should point to a trusted tracking URL, and pickup instructions should identify the actual collection location and hours.

`EmailTemplates.sample(name)` supplies complete illustrative models for admin previews. Sample order numbers and courier details are explicitly marked as examples. They must not be used for actual customer orders.

## Reusable fragments

`email/fragments/components.html` provides:

- `header`, `hero(headline)`, `button`, `footer`.
- `product(product)` and `products` (two-column grid): `products` is a list of up to two maps with `name`, `price`, `imageUrl`, `url`. Images are 4:5; prices are prefixed with “From”.
- `order`: the same order-summary model above.
- `discount`: `discountCode`, `discountExpiry` (display date).
- `review`: `quote`, `quoteAuthor` (a five-star quote component; use only for an actual five-star review).
- `founder`: `SITE_FOUNDER_NAME`, `SITE_FOUNDER_MESSAGE`, optional absolute `SITE_FOUNDER_PHOTO_URL`.

Use `th:replace="~{email/fragments/components :: button}"` and the same pattern for other fragments. The CTA includes a VML roundrect for Outlook and a regular anchor for other clients. Layout is table-based, maximum 600px, inline styles, with 48px+ CTA height, a preheader and light colour-scheme metadata.

## Local verification

Start `mailpit --listen 127.0.0.1:8025 --smtp 127.0.0.1:1025`. Run:

```sh
RUN_MAILPIT_TEST=true ./mvnw -Dtest=MailpitDeliveryTest test
```

This sends all six sample emails to `preview@example.test` through the local Mailpit capture server. It does not contact an external SMTP provider. View them at `http://127.0.0.1:8025`.

Normal `./mvnw test` renders previews to `target/email-previews/` and tests both MIME alternatives; it skips the opt-in SMTP test. The normal backend uses `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM`, and the existing SMTP credentials/settings. No persistent retry queue is added.
