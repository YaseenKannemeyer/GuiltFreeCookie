# 🍪 GuiltFreeCookie

A demo ecommerce shop for a small-batch cookie bakery: a **Vue 3** storefront (prerendered to static HTML) and a **Spring Boot 4** API on **Java 25** with MySQL.

It's a demo, not an operating bakery. Products are samples, payments are simulated and email is captured in an admin inbox.

What's still to do (hosting, monitoring, real providers): [docs/TODO.md](docs/TODO.md).

## Stack

- **Backend:** Java 25, Spring Boot 4, Spring Security + JWT, MySQL 8 (H2 in demo mode), Maven
- **Frontend:** Vue 3, Vite, Tailwind CSS, Axios

## Run the demo locally

Requires Java 25 and Node 22.12+.

```sh
cd frontend && npm ci && cd ..
bash scripts/run-demo.sh
```

- Storefront: http://127.0.0.1:5173
- Admin: http://127.0.0.1:5173/admin, sign in as `owner@example.com` / `DemoCookies123!` (local only)
- Discount code `DEMO10` takes 10% off
- Emails show up under Admin → Email previews → **Demo inbox**

The `demo` profile uses an isolated H2 database under `target/demo/` and never touches MySQL.

## Run against MySQL

```sh
cp .env.example .env          # fill in DB_* and JWT_SECRET (openssl rand -base64 32)
./mvnw spring-boot:run
cd frontend && npm run dev     # proxies /guiltFreeCookie to :8080
```

## Tests

```sh
./mvnw test
cd frontend && npm test
```

## API

Swagger UI is at `/guiltFreeCookie/swagger-ui.html` in development (set `API_DOCS_ENABLED=true` to turn it on in prod).

## License

[MIT](LICENSE)
