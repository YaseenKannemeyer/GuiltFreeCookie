# API Changelog

Base URL: `http://localhost:8080/guiltFreeCookie`. Swagger UI: `/swagger-ui.html`, OpenAPI JSON: `/v3/api-docs` (disabled in prod unless `API_DOCS_ENABLED=true`).

Auth: admin endpoints need `Authorization: Bearer <token>` from `POST /admin/login` (`{admin, token}`). Anything not listed as public in `SecurityConfig` requires an admin.

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
