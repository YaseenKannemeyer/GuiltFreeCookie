# API Changelog

Base URL: `http://localhost:8080/guiltFreeCookie`. Swagger UI: `/swagger-ui.html`, OpenAPI JSON: `/v3/api-docs` (disabled in prod unless `API_DOCS_ENABLED=true`).

Auth: admin endpoints need `Authorization: Bearer <token>` from `POST /admin/login` (`{admin, token}`). Anything not listed as public in `SecurityConfig` requires an admin.

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
