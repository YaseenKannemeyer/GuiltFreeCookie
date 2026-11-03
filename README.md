# 🍪 GuiltFreeCookie

[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)

**GuiltFreeCookie** is a demo ecommerce platform with a **Vue 3** storefront and a **Spring Boot 4** backend running on **Java 25**.

This repository is currently a **demo project**, not an operating bakery. Run `bash scripts/run-demo.sh` for a local shop with 12 sample products, an isolated database, simulated payments and a captured email inbox. See [docs/DEMO.md](docs/DEMO.md) for the demo login, checks and the Vercel/Azure hosting setup.

---

## 🛠 Tech Stack

### Backend 🖥️
- **Language:** ![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
- **Framework:** ![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.3.2-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)
- **Security:** ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
- **Token Auth:** ![JWT](https://img.shields.io/badge/JWT-black?style=flat-square&logo=JSON%20web%20tokens)
- **Database:** ![MySQL](https://img.shields.io/badge/MySQL_8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)
- **Build Tool:** ![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apache-maven&logoColor=white)

### Frontend 🎨
- **Framework:** ![Vue 3](https://img.shields.io/badge/Vue_3-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white)
- **Build Tool:** ![Vite](https://img.shields.io/badge/Vite-646CFF?style=flat-square&logo=vite&logoColor=white)
- **Styling:** ![Tailwind](https://img.shields.io/badge/Tailwind_CSS-38B2AC?style=flat-square&logo=tailwind-css&logoColor=white)
- **HTTP Client:** ![Axios](https://img.shields.io/badge/Axios-5A29E4?style=flat-square&logo=axios&logoColor=white)

---

## 📂 Project Architecture

A Spring Boot REST API plus a Vue 3 storefront that is prerendered to static HTML at build time.

### Backend (`za.ac.cput.guiltfreecookie`)
- **`config/`**: security filter chain, CORS, headers, upload serving.
- **`controller/`**: REST endpoints (catalogue, admins, customers, newsletter).
- **`domain/`**: JPA entities using the **Builder Pattern**.
- **`repository/`**: Spring Data repositories (no hand-written SQL).
- **`security/`**: JWT authentication and per-IP rate limiting.
- **`service/`**: business logic and image storage.

### Frontend (`frontend/src`)
- **`api/`**: Axios calls; the base URL comes from `VITE_API_BASE_URL`.
- **`router/`**: routes plus each page's title, description and breadcrumb.
- **`seo/`**: head tags, structured data, sitemap, robots.txt and llms.txt.
- **`views/`, `components/`, `layout/`**: pages and UI.
- **`entry-server.js` + `scripts/prerender.mjs`**: build-time rendering of every public page.

---

## 🚀 Getting Started

### 1. Secrets
No secrets live in the repository. Copy `.env.example` to `.env` (git-ignored) and fill it in:
```bash
cp .env.example .env
openssl rand -base64 32   # paste the output as JWT_SECRET
```

### 2. Run the backend
```bash
./mvnw spring-boot:run
```
The database (`guiltFreeCookie`) is created on first run.

### 3. Run the frontend
```bash
cd frontend
npm install
npm run dev        # http://localhost:5173, proxies /guiltFreeCookie to :8080
```

### 4. Production build
```bash
cd frontend
PRERENDER_API_URL=https://<your-api>/guiltFreeCookie npm run build
```
This writes `dist/` with one real HTML page per URL (`about.html`, `product/<id>.html`, ...), plus `404.html`, `sitemap.xml`, `robots.txt` and `llms.txt`. Set `VITE_SITE_URL` if the domain isn't `https://guiltfreecookie.co.za`. Without `PRERENDER_API_URL`, product pages are left out of the prerender and the sitemap. Rebuild whenever products change.

---

## 🔒 Deploying safely

The full go-live checklist, every environment variable and the first-admin setup are in [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

**Backend:** run with `SPRING_PROFILES_ACTIVE=prod` and set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` and `CORS_ALLOWED_ORIGINS` (the site's origin). The prod profile validates the schema instead of altering it (set `JPA_DDL_AUTO=update` for the very first deploy only), redirects HTTP to HTTPS, and shortens admin sessions to 8 hours. Use a database user with access to this schema only, not `root`.

**Frontend host:** serve `dist/` over HTTPS, serve `404.html` for unknown paths, and route `/guiltFreeCookie/*` to the API (or build with `VITE_API_BASE_URL`). Add these response headers at the host; a `<meta>` tag can't set them:
```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()
```
The Content-Security-Policy is already in every page.

**Costs:** nothing in the code calls a paid API. Set a budget alert or spend cap with your hosting and database provider; the API's rate limits and 10 MB upload cap keep abusive traffic cheap.

---

## 🍪 Key Features

- ✅ **Real HTML for every page**: unique titles, descriptions, canonical links, breadcrumbs, structured data and share images.
- ✅ **Secure admin**: JWT sessions that end on password change or deactivation, rate-limited login, no password hashes in responses.
- ✅ **Safe uploads**: images are checked by their bytes, not their name.
- ✅ **Mobile Optimized**: Sidebar drawer for smaller screens.

---

## 📜 License
Licensed under the [MIT License](LICENSE).
