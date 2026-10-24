const assert = require("node:assert/strict");
const fs = require("node:fs");
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const origin = process.env.TEST_ORIGIN || "http://127.0.0.1:5173";
const customer = {
  customerEmail: "sam@example.com",
  name: { firstName: "Sam", lastName: "Baker" },
  mobileNumber: "0821234567",
};
const summary = {
  cookieId: "c1",
  slug: "double-chocolate",
  name: "Double Chocolate Chunk",
  category: "LOW_SUGAR",
  image: "cookies/chocolate.webp",
  fromPrice: 34,
  featured: true,
  stockStatus: "LOW_STOCK",
  averageRating: 4.5,
  reviewCount: 2,
};
const variant = {
  variantId: "v1",
  label: "Single",
  unitsPerPack: 1,
  price: 34,
  pricePerCookie: 34,
  available: true,
  active: true,
  sortOrder: 0,
};
const detail = {
  ...summary,
  longDescription:
    "Deep cocoa, dark chocolate chunks and a soft, tender centre.",
  ingredients: "Almond flour, cocoa",
  allergies: "Almonds",
  lowStockRemaining: 8,
  images: [
    {
      imageId: "i1",
      image: "cookies/lemon.webp",
      altText: "Cookie detail",
      sortOrder: 0,
    },
  ],
  variants: [
    variant,
    {
      ...variant,
      variantId: "v12",
      label: "Box of 12",
      unitsPerPack: 12,
      available: false,
    },
  ],
  nutrition: {
    calories: 160,
    proteinGrams: 6,
    sugarGrams: 2.5,
    carbsGrams: null,
  },
  related: [],
};
const review = {
  id: "r1",
  cookieId: "c1",
  rating: 5,
  title: "A lovely afternoon treat",
  body: "Full of chocolate flavour and just the right amount of crunch.",
  authorName: "Sam",
  verifiedBuyer: false,
  status: "PENDING",
  createdAt: "2026-10-24T12:00:00",
};
const adminCookie = {
  cookie: {
    ...summary,
    description: summary.name,
    price: 34,
    stockQuantity: 8,
    lowStockThreshold: 12,
  },
  variants: [variant],
  images: detail.images,
  stockStatus: "LOW_STOCK",
};
(async () => {
  const browser = await chromium.launch({
    headless: true,
    ...(process.env.CHROME_PATH
      ? { executablePath: process.env.CHROME_PATH }
      : {}),
  });
  fs.mkdirSync("target/phase6-browser", { recursive: true });
  const failures = [];
  const requests = [];
  for (const width of [390, 1440]) {
    const context = await browser.newContext({
      viewport: { width, height: 1000 },
      reducedMotion: "reduce",
    });
    await context.addInitScript(
      ({ customer }) => {
        if (window !== window.top) return;
        localStorage.setItem(
          "gfc_admin_session",
          JSON.stringify({
            admin: {
              adminId: "a1",
              firstName: "Alex",
              lastName: "Baker",
              email: "admin@example.com",
            },
            token: "admin-test-token",
          }),
        );
        sessionStorage.setItem(
          "gfc_customer_session",
          JSON.stringify({ customer, token: "customer-test-token" }),
        );
      },
      { customer },
    );
    await context.route("https://fonts.googleapis.com/**", (route) =>
      route.abort(),
    );
    await context.route("https://fonts.gstatic.com/**", (route) =>
      route.abort(),
    );
    await context.route("**/guiltFreeCookie/**", async (route) => {
      const request = route.request(),
        url = new URL(request.url()),
        path = url.pathname.replace("/guiltFreeCookie", "");
      requests.push({
        path,
        method: request.method(),
        auth: request.headers().authorization,
      });
      if (path.startsWith("/uploads/"))
        return route.fulfill({
          status: 200,
          contentType: "image/webp",
          body: fs.readFileSync(
            "frontend/public/img/cookies/double-chocolate-chunk.webp",
          ),
        });
      let data = {},
        status = 200,
        type = "application/json";
      if (path === "/api/v1/site/info")
        data = {
          supportEmail: "hello@example.com",
          phone: "0821234567",
          hours: "Mon–Fri 08:00–17:00",
          responseTime: "We reply within 1 business day",
          address: "Cape Town, South Africa",
          social: {},
          stats: {
            averageRating: 4.5,
            reviewCount: 2,
            customersServed: null,
            cookiesSold: null,
          },
        };
      else if (path === "/api/v1/cookies/categories")
        data = [{ category: "LOW_SUGAR", label: "Low sugar", count: 1 }];
      else if (path === "/api/v1/cookies")
        data = {
          items: [summary],
          page: 0,
          size: 12,
          totalItems: 1,
          totalPages: 1,
        };
      else if (path.endsWith("/reviews") && path.startsWith("/api/v1/cookies/"))
        data = {
          summary: {
            averageRating: 5,
            reviewCount: 1,
            histogram: { 5: 1, 4: 0, 3: 0, 2: 0, 1: 0 },
          },
          page: {
            items: [{ ...review, status: "APPROVED" }],
            page: 0,
            totalPages: 1,
            totalItems: 1,
          },
        };
      else if (path.startsWith("/api/v1/cookies/")) data = detail;
      else if (path === "/api/v1/reviews/featured") data = [review];
      else if (path === "/api/v1/me") data = customer;
      else if (path === "/api/v1/me/password")
        data = { customer, token: "customer-updated-token" };
      else if (path === "/api/v1/contact") status = 204;
      else if (
        path === "/api/v1/admin/cookies" ||
        path === "/api/v1/admin/inventory/low-stock"
      )
        data = [adminCookie];
      else if (path.endsWith("/inventory"))
        data = [
          {
            quantityChange: 8,
            stockAfter: 8,
            reason: "RESTOCK",
            note: "Fresh batch",
            actor: "a1",
            createdAt: review.createdAt,
          },
        ];
      else if (path.startsWith("/api/v1/admin/cookies/")) data = adminCookie;
      else if (path === "/api/v1/admin/messages")
        data = [
          {
            id: "m1",
            name: "Sam",
            email: "sam@example.com",
            subject: "Bulk order",
            message: "Could I order cookies for our office?",
            status: "NEW",
            createdAt: review.createdAt,
          },
        ];
      else if (path === "/api/v1/admin/reviews") data = [review];
      else if (path === "/api/v1/admin/email-previews")
        data = [
          "password-reset",
          "contact-auto-reply",
          "contact-to-shop",
          "order-confirmation",
          "order-shipped",
          "order-ready-for-pickup",
        ];
      else if (path.startsWith("/api/v1/admin/email-previews/")) {
        type = "text/html";
        data = fs.readFileSync(
          "target/email-previews/" + path.split("/").at(-1) + ".html",
          "utf8",
        );
      } else if (path.startsWith("/api/v1/me/reviews/")) {
        data = review;
        status = 201;
      } else if (path === "/cookie/getAllForAdmin") data = [adminCookie.cookie];
      return route.fulfill({
        status,
        contentType: type,
        body:
          status === 204
            ? ""
            : type === "application/json"
              ? JSON.stringify(data)
              : data,
      });
    });
    const page = await context.newPage();
    page.on("pageerror", (e) => failures.push(`${width}: ${e.message}`));
    for (const path of [
      "/login",
      "/register",
      "/forgot-password",
      "/reset-password?token=sample",
      "/account",
      "/products",
      "/product/c1",
      "/contact",
      "/admindashboard/inventory",
      "/admindashboard/messages",
      "/admindashboard/reviews",
      "/admindashboard/email-previews",
    ]) {
      await page.goto(origin + path);
      await page.waitForTimeout(550);
      const overflow = await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 1,
      );
      assert.equal(overflow, false, `Overflow at ${width} ${path}`);
      if (path === "/product/c1") {
        await page.waitForURL("**/product/double-chocolate");
        assert.equal(
          await page.getByRole("radio", { name: /Box of 12/ }).isDisabled(),
          true,
        );
      }
      if (path === "/admindashboard/email-previews") {
        assert.equal(await page.locator("iframe").getAttribute("sandbox"), "");
        await page
          .frameLocator("iframe")
          .getByRole("heading", { name: "A fresh start" })
          .waitFor();
      }
      if (!process.env.NO_SCREENSHOTS)
        await page.screenshot({
          path: `target/phase6-browser/${width}-${path.split("?")[0].replaceAll("/", "_")}.png`,
          fullPage: true,
        });
    }
    await page.goto(origin + "/contact");
    await page.getByLabel("Your name").fill("Sam");
    await page.getByLabel("Your email").fill("sam@example.com");
    await page.getByLabel(/^Subject/).fill("Question");
    await page
      .getByLabel(/^Message/)
      .fill("Could you tell me about the cookies?");
    await page
      .getByRole("button", { name: "Send message", exact: true })
      .click();
    await page.getByRole("heading", { name: "Message received" }).waitFor();
    await page.goto(origin + "/products");
    await page.getByRole("searchbox").fill("chocolate");
    await page.waitForTimeout(700);
    assert.ok(requests.some((r) => r.path === "/api/v1/cookies"));
    await context.close();
  }
  assert.deepEqual(failures, []);
  for (const request of requests) {
    if (request.path.startsWith("/api/v1/me"))
      assert.equal(request.auth, "Bearer customer-test-token");
    if (request.path.startsWith("/api/v1/admin"))
      assert.equal(request.auth, "Bearer admin-test-token");
    if (
      request.path === "/api/v1/contact" ||
      request.path === "/api/v1/site/info"
    )
      assert.equal(request.auth, undefined);
  }
  console.log(
    "PASS: 12 routes at phone and desktop widths, contact submission, slug redirect, unavailable packs, sandboxed previews and token separation.",
  );
  await browser.close();
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
