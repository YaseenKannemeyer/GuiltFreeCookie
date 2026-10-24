# API Changelog

Base URL: `http://localhost:8080/guiltFreeCookie`. Swagger UI: `/swagger-ui.html`, OpenAPI JSON: `/v3/api-docs` (disabled in prod unless `API_DOCS_ENABLED=true`).

Auth: admin endpoints need `Authorization: Bearer <token>` from `POST /admin/login` (`{admin, token}`). Anything not listed as public in `SecurityConfig` requires an admin.

## 2026-10-24 — Phase 3: Cart & discount codes

Guest-first cart. **Store `cartId` in localStorage**; there's no login needed. Every cart endpoint returns the full priced cart (`CartView`), so just re-render from the response. Prices always come from the server.

### Storefront (public; a customer token is optional)

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/api/v1/carts` | – | `201 CartView`. With a customer token it returns their existing cart if there is one. Rate-limited (30/hour per IP) |
| GET | `/api/v1/carts/{cartId}` | – | `404` if it doesn't exist: start a new cart |
| POST | `/api/v1/carts/{cartId}/items` | `{variantId, quantity}` | Adds a pack (`variantId` from product detail). The same pack again increases its quantity |
| PUT | `/api/v1/carts/{cartId}/items/{cartItemId}` | `{quantity}` | `0` removes the item |
| DELETE | `/api/v1/carts/{cartId}/items/{cartItemId}` | – | Remove one item |
| DELETE | `/api/v1/carts/{cartId}/items` | – | Empty the cart (also drops the discount code) |
| POST | `/api/v1/carts/{cartId}/discount` | `{code}` | Case-insensitive. `400` with a message to show ("This code has expired", "Spend R150.00 or more to use this code", …) |
| DELETE | `/api/v1/carts/{cartId}/discount` | – | |
| PUT | `/api/v1/carts/{cartId}/email` | `{email}` | Guests: ask for the email **first** in checkout (it enables cart-recovery emails). `409` for a signed-in customer's cart |

### Signed-in customers (customer token)

| Method | Path | Body | Notes |
|---|---|---|---|
| GET | `/api/v1/me/cart` | – | Their saved cart; `404` if none |
| POST | `/api/v1/me/cart/merge` | `{cartId}` | **Call right after login or registration** with the browser's cartId. It returns the cart to use from now on (it can be a different `cartId`: store the returned one) |

### CartView
```json
{
  "cartId": "…", "status": "ACTIVE", "email": "thandi@example.co.za", "accountOwned": false,
  "items": [
    { "cartItemId": "…", "cookieId": "…", "slug": "double-chocolate-chunk", "name": "Double Chocolate Chunk",
      "image": "cookies/….webp", "variantId": "…", "variantLabel": "Box of 6", "unitsPerPack": 6,
      "unitPrice": 170.00, "quantity": 1, "lineTotal": 170.00, "maxQuantity": 1, "issue": null }
  ],
  "itemCount": 3, "cookieCount": 8,
  "subtotal": 234.00,
  "discount": { "code": "TREAT10", "type": "PERCENT", "description": "10% off your treat", "amount": 23.40,
                "freeShipping": false, "applied": true, "message": null },
  "discountTotal": 23.40,
  "shipping": null,
  "vatIncluded": null,
  "total": 210.60, "currency": "ZAR",
  "canCheckout": true
}
```
- **Quantity selector:** limit it to `maxQuantity`, which already accounts for other pack sizes of the same cookie in the cart (max 50).
- **`issue`** on a line:
  - `NOT_ENOUGH_STOCK`: stock dropped after adding. Show "Only N available", offer to set the quantity to `maxQuantity`, or remove it.
  - `UNAVAILABLE`: the cookie was archived or the pack hidden. It's priced at 0; offer "Remove".
- **`canCheckout: false`** while any line has an issue or the cart is empty. Disable the checkout button.
- **`discount.applied: false`:** the code is still on the cart but isn't counting right now. Show `discount.message` (e.g. the subtotal dropped below the minimum).
- **`shipping`** is `null` until Phase 4: show "Calculated at checkout". `vatIncluded` is `null` unless the shop is VAT-registered.
- **Errors:**
  - `409` when adding more than is in stock. Show `message`, e.g. "Sorry, you can only add 2 more Double Chocolate Chunk cookies" or "… is sold out".
  - `409` when changing a checked-out cart. Start a new cart.
  - `404` for an unknown pack or cookie.
  - `400` for a quantity outside 1–50 or more than 30 different items.

### Admin: discount codes (admin token)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/admin/discounts` | Newest first |
| GET | `/api/v1/admin/discounts/{id}` | |
| POST | `/api/v1/admin/discounts` | See the body below. `409` if the code exists |
| PUT | `/api/v1/admin/discounts/{id}` | Same body; replaces everything except `usedCount` |
| DELETE | `/api/v1/admin/discounts/{id}` | Switches it off (`active: false`); it isn't deleted |

Body: `{code, type, value, minSubtotal?, startsAt?, endsAt?, maxUses?, onePerEmail?, restrictedToEmail?, description?, active?}`
- `type`: `PERCENT` (`value` 1–100), `FIXED` (rand), `FREE_SHIPPING` (no value; it takes effect on shipping in Phase 4)
- `code`: 3–30 letters, numbers, `-` or `_`; stored in upper case
- `maxUses: 1` makes a single-use code. `restrictedToEmail` makes a personal code. Leave the dates empty for "always". Dates are local time (`2026-12-01T00:00:00`)
- A use is counted when an order is paid (Phase 4), not when the code is applied to a cart

### Unchanged
`/cart/*` (admin) still works and now shows the server-computed `totalPrice` of storefront carts.

## 2026-10-24 — Phase 2: Catalog

Use these endpoints for the storefront from now on. `/cookie/getAll` and `/cookie/read/{id}` still work but return the raw entity.

Image paths (`image`) are relative, exactly as before: build URLs with `cookieApi.getImageUrl(image)`. A cookie's **name is `name`** in these responses (it's `description` in the legacy entity).

### Public

**`GET /api/v1/cookies`**: search, filter and sort; paged.

| Query param | Notes |
|---|---|
| `q` | Matches name, long description and ingredients (case-insensitive, max 100 chars) |
| `category` | `LOW_SUGAR`, `DIABETIC_FRIENDLY`, `HIGH_PROTEIN`, `GLUTEN_FREE`, `VEGAN`, `CLASSIC` |
| `minPrice`, `maxPrice` | Unit price in rand |
| `inStock=true` | Hide sold-out cookies |
| `featured=true` | Homepage "featured" row |
| `sort` | `featured` (default: featured first, then A–Z), `price_asc`, `price_desc`, `newest`, `name` |
| `page`, `size` | `page` starts at 0; `size` defaults to 12, max 48 |

Response: `{items: CookieSummary[], page, size, totalItems, totalPages}`

`CookieSummary` = `{cookieId, slug, name, category, image, fromPrice, featured, stockStatus, averageRating, reviewCount}`
- `stockStatus`: `IN_STOCK` | `LOW_STOCK` | `OUT_OF_STOCK`
- `averageRating` is `null` and `reviewCount` is `0` until reviews ship. Hide the stars when the count is 0.

**`GET /api/v1/cookies/{idOrSlug}`**: product detail. Use the slug in URLs (`/product/double-chocolate-chunk`).

```json
{
  "cookieId": "…", "slug": "double-chocolate-chunk", "name": "Double Chocolate Chunk",
  "category": "LOW_SUGAR", "longDescription": "…", "ingredients": "…", "allergies": "…",
  "image": "cookies/….webp",
  "images": [ { "imageId": "…", "image": "cookies/….jpg", "altText": "…", "sortOrder": 0 } ],
  "variants": [
    { "variantId": "…", "label": "Single",    "unitsPerPack": 1,  "price": 34.00,  "pricePerCookie": 34.00, "available": true },
    { "variantId": "…", "label": "Box of 12", "unitsPerPack": 12, "price": 330.00, "pricePerCookie": 27.50, "available": false }
  ],
  "fromPrice": 34.00, "featured": true,
  "stockStatus": "LOW_STOCK", "lowStockRemaining": 8,
  "nutrition": { "weightGrams": 40, "calories": 160, "sugarGrams": 2.5, "proteinGrams": 6.0, "carbsGrams": null, "fatGrams": null },
  "averageRating": null, "reviewCount": 0,
  "related": [ CookieSummary, … up to 4 ]
}
```
- Gallery = `image` (primary) followed by `images[]`.
- Pack picker: show the `variants` and disable the ones with `available: false`. The cart (Phase 3) takes a `variantId`.
- `lowStockRemaining` is only set when the status is `LOW_STOCK` ("Only 8 left").
- Nutrition values are per cookie, and any of them can be `null`: hide what's missing.
- `404` for unknown or archived cookies. Show the 404 page.

**`GET /api/v1/cookies/categories`** returns `[{category, label, count}]` for every category. Labels match `CATEGORY_META.short`.

### Admin (admin token)

The existing admin form keeps using `/cookie/create|update|archive|delete`. The new fields can't be set through `/cookie/*`; use these:

| Method | Path | Body / notes |
|---|---|---|
| GET | `/api/v1/admin/cookies` | All cookies (archived too): `[{cookie, stockStatus, variants, images}]` |
| GET | `/api/v1/admin/cookies/{id}` | One of the above |
| PUT | `/api/v1/admin/cookies/{id}/details` | `{slug?, featured, lowStockThreshold?, weightGrams?, calories?, sugarGrams?, proteinGrams?, carbsGrams?, fatGrams?}`. Replaces all of these; `slug` is kept if omitted |
| PUT | `/api/v1/admin/cookies/{id}/stock` | `{adjustBy: 24}` or `{setTo: 30}`, optional `reason` (`RESTOCK`, `ADJUSTMENT`, `STOCKTAKE`, `DAMAGED`) and `note`. `409` if stock would go below 0 |
| GET | `/api/v1/admin/cookies/{id}/inventory?limit=50` | Stock history: `[{quantityChange, stockAfter, reason, note, actor, createdAt}]` |
| GET | `/api/v1/admin/inventory/low-stock` | Low and sold-out cookies, most urgent first (dashboard widget) |
| GET/POST | `/api/v1/admin/cookies/{id}/variants` | POST `{label, unitsPerPack, price?, active?, sortOrder?}`. Leave `price` empty to charge unit price × pack size |
| PUT | `/api/v1/admin/cookies/{id}/variants/{variantId}` | Same body |
| DELETE | `/api/v1/admin/cookies/{id}/variants/{variantId}` | Hides the pack (`active: false`); it isn't deleted |
| POST | `/api/v1/admin/cookies/{id}/images` | multipart: `image` file and optional `altText`. Max 10 per cookie |
| PUT | `/api/v1/admin/cookies/{id}/images/order` | `{imageIds: [...]}`: every id, in display order |
| DELETE | `/api/v1/admin/cookies/{id}/images/{imageId}` | |

### Behaviour changes
- **Every cookie starts with 0 stock** (shown as `OUT_OF_STOCK`) until stock is set through `/stock`. Existing cookies included.
- Every cookie gets a slug and a "Single" pack automatically (new cookies on create, existing ones on startup).
- A cookie is "low stock" at 12 or fewer, unless it has its own `lowStockThreshold`.
- Hard-deleting a cookie (`/cookie/delete`) also removes its packs and gallery photos.

## 2026-10-24 — Phase 1: Customer accounts

Accounts are optional; guest checkout (Phase 4) never requires one. Customer tokens work exactly like admin tokens: send `Authorization: Bearer <token>`. Store the token the same way the admin session is stored.

**Added (public, rate-limited together: 10 requests / 5 min per IP)**

| Method | Path | Body | Success |
|---|---|---|---|
| POST | `/api/v1/auth/register` | `{email, password, firstName, middleName?, lastName, mobileNumber?, newsletterOptIn?}` | `201 {customer, token}` |
| POST | `/api/v1/auth/login` | `{email, password}` | `200 {customer, token}` |
| POST | `/api/v1/auth/forgot-password` | `{email}` | `204` always (never reveals whether the email exists) |
| POST | `/api/v1/auth/reset-password` | `{token, newPassword}` | `200 {customer, token}` (signs the customer in) |

**Added (customer token required)**

| Method | Path | Body | Success |
|---|---|---|---|
| GET | `/api/v1/me` | – | `200 customer` |
| PUT | `/api/v1/me` | `{firstName, middleName?, lastName, mobileNumber?}` | `200 customer` |
| PUT | `/api/v1/me/password` | `{currentPassword, newPassword}` | `200 {customer, token}` (replace the stored token; other sessions end) |

`customer` = `{customerEmail, name: {firstName, middleName, lastName}, mobileNumber, createdAt}`.

**Errors** use the standard error body. Show `message` to the user:
- `400` validation (bad email, password under 8 characters, missing name, invalid SA mobile number, bad or expired reset link)
- `401` wrong email or password, or wrong current password
- `409` email already registered (the message points the user to "Forgot password")
- `429` too many attempts (`Retry-After` header in seconds)

**Frontend pages needed:** Login, Register, Forgot password, Reset password (reads `?token=` from the emailed link `/reset-password?token=...`), My account (profile and change password).

**Notes**
- Customer tokens are rejected by admin endpoints (`403`), and admin tokens are rejected by `/api/v1/me` (`403`).
- A `401` on `/api/v1/me` means the session ended (password changed elsewhere). Send the user to login.
- `/api/v1/customers` (admin) never returns password data.

## 2026-10-24 — Phase 0: Foundation

**Changed**
- `GET /api/v1/delivery-details/read/{id}` returns `404` when the record doesn't exist (was `200` with an empty body).
- `PUT /api/v1/delivery-details/update` returns `404` for an unknown id (was a silent create).
- `POST /api/v1/delivery-details/create` returns `409` if the id already exists (was a silent overwrite).

**Added**
- Swagger UI and OpenAPI docs, with a bearer-token "Authorize" button.
- New endpoints added from Phase 1 onward return errors in one shape:
  ```json
  { "timestamp": "...", "status": 404, "error": "Not Found", "message": "...", "fieldErrors": [ { "field": "email", "message": "..." } ] }
  ```
  Existing endpoints keep their current responses (empty body or plain-text message).

**No breaking changes** to `/cookie/*`, `/admin/*`, `/newsletter/*`, `/cart/*` or `/api/v1/customers`.
