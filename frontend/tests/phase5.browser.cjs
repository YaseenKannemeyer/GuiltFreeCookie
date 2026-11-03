// Run with PLAYWRIGHT_MODULE pointing at Playwright and Vite on localhost:5175.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const base = process.env.TEST_BASE_URL || "http://127.0.0.1:5175";
const customer = {
  customerEmail: "shopper@example.test",
  name: { firstName: "Thandi", lastName: "Mokoena" },
  mobileNumber: "0821234567",
};
const address = {
  streetNumber: "12",
  streetName: "Bakery Lane",
  complex: "",
  suburb: "Gardens",
  city: "Cape Town",
  province: "WESTERN_CAPE",
  postalCode: "0002",
};
const saved = (id, label, isDefault) => ({
  id,
  label,
  isDefault,
  recipientName: "Thandi Mokoena",
  phone: "0821234567",
  createdAt: "2026-10-25T10:00:00",
  address: { ...address, streetNumber: id === "work" ? "20" : "12" },
});
const order = (n) => ({
  orderNumber: `GFC-TEST-${n}`,
  createdAt: "2026-10-25T10:00:00",
  status: "PAID",
  shippingMethod: "COURIER",
  itemCount: 1,
  total: 180,
  image: null,
  token: `signed-${n}`,
});
(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath:
      process.env.CHROME_PATH ||
      "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
  });
  fs.mkdirSync("target/phase5-browser", { recursive: true });
  try {
    for (const width of [390, 1280]) {
      const context = await browser.newContext({
        viewport: { width, height: 900 },
      });
      await context.addInitScript(
        (c) =>
          localStorage.setItem(
            "gfc_customer_session",
            JSON.stringify({ customer: c, token: "customer-token" }),
          ),
        customer,
      );
      const page = await context.newPage();
      page.setDefaultTimeout(10000);
      const errors = [];
      page.on("pageerror", (e) => errors.push(e.message));
      let addresses = [
          saved("home", "Home", true),
          saved("work", "Work", false),
        ],
        empty = false,
        failSave = false,
        checkoutBody = null,
        saves = 0;
      await page.route("**/guiltFreeCookie/**", async (route) => {
        const req = route.request(),
          url = new URL(req.url()),
          path = url.pathname.split("/api/v1")[1],
          method = req.method();
        const send = (body, status = 200) =>
          route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify(body),
          });
        if (path?.startsWith("/me"))
          assert.equal(req.headers().authorization, "Bearer customer-token");
        if (path === "/me") return send(customer);
        if (path === "/me/orders/claimable")
          return send({ count: empty ? 0 : 2 });
        if (path === "/me/orders/claim") return route.fulfill({ status: 204 });
        if (path === "/me/orders/claim/confirm")
          return req.postDataJSON().token === "bad"
            ? send({ message: "Claim link is invalid or expired" }, 400)
            : send({ claimed: 2 });
        if (path === "/me/orders") {
          const p = Number(url.searchParams.get("page"));
          return send({
            items: empty ? [] : [order(p + 1)],
            page: p,
            size: 10,
            totalItems: empty ? 0 : 2,
            totalPages: empty ? 0 : 2,
          });
        }
        if (path === "/me/addresses" && method === "GET")
          return send(addresses);
        if (path === "/me/addresses" && method === "POST") {
          saves++;
          if (failSave) return send({ message: "Expired session" }, 401);
          const a = {
            ...req.postDataJSON(),
            id: "new",
            createdAt: "2026-10-25T11:00:00",
          };
          addresses.push(a);
          return send(a, 201);
        }
        if (path?.startsWith("/me/addresses/")) {
          const id = path.split("/")[3];
          if (path.endsWith("/default")) {
            addresses.forEach((a) => (a.isDefault = a.id === id));
            return send(addresses.find((a) => a.id === id));
          }
          if (method === "DELETE") {
            const was = addresses.find((a) => a.id === id).isDefault;
            addresses = addresses.filter((a) => a.id !== id);
            if (was && addresses.length) addresses.at(-1).isDefault = true;
            return route.fulfill({ status: 204 });
          }
          if (method === "PUT") {
            addresses = addresses.map((a) =>
              a.id === id ? { ...a, ...req.postDataJSON() } : a,
            );
            return send(addresses.find((a) => a.id === id));
          }
        }
        if (path?.endsWith("/shipping-options"))
          return send([
            {
              method: "COURIER",
              label: "Courier to your door",
              cost: 80,
              eta: "2–4 days",
              requiresAddress: true,
            },
          ]);
        if (path?.startsWith("/carts/"))
          return send({
            cartId: "cart",
            status: "ACTIVE",
            items: [
              {
                itemId: "item",
                cookieId: "cookie",
                name: "Chocolate cookie",
                variantLabel: "Single",
                unitsPerPack: 1,
                quantity: 1,
                lineTotal: 100,
              },
            ],
            itemCount: 1,
            subtotal: 100,
            discountTotal: 0,
            shipping: 80,
            total: 180,
            canCheckout: true,
          });
        if (path === "/checkout") {
          checkoutBody = req.postDataJSON();
          return send({ message: "Payment request reached" }, 503);
        }
        if (path === "/site/info") return send({});
        return send({});
      });
      const noOverflow = async () =>
        assert(
          await page.evaluate(
            () => document.documentElement.scrollWidth <= innerWidth,
          ),
          `overflow at ${width}: ${page.url()}`,
        );
      await page.goto(base + "/account");
      await page.getByText("GFC-TEST-1", { exact: true }).waitFor();
      assert(
        (
          await page
            .locator("#orders a")
            .filter({ hasText: "GFC-TEST-1" })
            .getAttribute("href")
        ).includes("token=signed-1"),
      );
      await page
        .getByRole("button", { name: "Load more", exact: true })
        .click();
      await page.getByText("GFC-TEST-2", { exact: true }).waitFor();
      await page.getByRole("button", { name: "Send me a link" }).click();
      await page.getByText("Check your inbox.", { exact: false }).waitFor();
      const work = page
        .locator("#addresses article")
        .filter({
          has: page.getByRole("heading", { name: "Work", exact: true }),
        });
      await work.getByRole("button", { name: "Make default" }).click();
      await work.getByText("Default", { exact: true }).waitFor();
      await work.getByRole("button", { name: "Edit", exact: true }).click();
      await page
        .getByRole("textbox", { name: "Label", exact: true })
        .fill("Office");
      await page
        .getByRole("button", { name: "Save address", exact: true })
        .click();
      await page
        .getByRole("heading", { name: "Office", exact: true })
        .waitFor();
      await page
        .getByRole("button", { name: "Add address", exact: true })
        .click();
      for (const [label, value] of [
        ["Label", "Parents"],
        ["Number", "3"],
        ["Street", "Long Street"],
        ["Suburb", "Gardens"],
        ["City or town", "Cape Town"],
        ["Postal code", "0002"],
      ])
        await page
          .locator("#addresses")
          .getByRole("textbox", { name: label, exact: true })
          .fill(value);
      await noOverflow();
      await page.screenshot({
        path: `target/phase5-browser/account-editor-${width}.png`,
        fullPage: true,
      });
      await page
        .getByRole("button", { name: "Save address", exact: true })
        .click();
      await page
        .getByRole("heading", { name: "Parents", exact: true })
        .waitFor();
      const office = page
        .locator("#addresses article")
        .filter({
          has: page.getByRole("heading", { name: "Office", exact: true }),
        });
      await office.getByRole("button", { name: "Delete", exact: true }).click();
      await office.getByText("Delete this address?", { exact: true }).waitFor();
      assert.equal(addresses.length, 3);
      await office.getByRole("button", { name: "Yes, delete" }).click();
      await page.getByText("Address deleted.", { exact: true }).waitFor();
      assert.equal(addresses.length, 2);
      assert(addresses.find((a) => a.id === "new").isDefault);
      await noOverflow();
      await page.screenshot({
        path: `target/phase5-browser/account-${width}.png`,
        fullPage: true,
      });
      await page.goto(base + "/account/claim?token=valid");
      await page
        .getByText("2 orders added to your account.", { exact: false })
        .waitFor();
      await noOverflow();
      await page.goto(base + "/account/claim?token=bad");
      await page
        .getByRole("alert")
        .filter({ hasText: "invalid or expired" })
        .waitFor();
      empty = true;
      await page.goto(base + "/account");
      await page
        .getByText("Your orders will appear here once you place one.")
        .waitFor();
      assert.equal(
        await page
          .locator("#orders")
          .getByRole("link", { name: "Browse cookies" })
          .getAttribute("href"),
        "/products",
      );
      addresses = [saved("home", "Home", true), saved("work", "Work", false)];
      await page.evaluate(() => {
        localStorage.setItem("gfc_cart_id", "cart");
        sessionStorage.removeItem("gfc_checkout_draft");
      });
      await page.goto(base + "/checkout/delivery");
      await page
        .locator("label")
        .filter({
          has: page.getByRole("radio", { name: /Courier to your door/ }),
        })
        .click();
      await page
        .getByRole("textbox", { name: "Street", exact: true })
        .waitFor();
      assert.equal(
        await page
          .getByRole("textbox", { name: "Number", exact: true })
          .inputValue(),
        "12",
      );
      await page
        .locator("label")
        .filter({ has: page.getByRole("radio", { name: /Work/ }) })
        .click();
      assert.equal(
        await page
          .getByRole("textbox", { name: "Number", exact: true })
          .inputValue(),
        "20",
      );
      assert.equal(
        await page
          .getByRole("textbox", { name: "Postal code", exact: true })
          .inputValue(),
        "0002",
      );
      await page.getByLabel("Save this address to my account").check();
      await noOverflow();
      await page.screenshot({
        path: `target/phase5-browser/checkout-${width}.png`,
        fullPage: true,
      });
      await page.getByRole("button", { name: "Continue to payment" }).click();
      await page.waitForURL("**/checkout/pay");
      failSave = true;
      const before = saves;
      await page.getByRole("button", { name: /^Pay R/ }).click();
      await page
        .getByText("Payment request reached", { exact: true })
        .waitFor();
      assert.equal(saves, before + 1);
      assert.equal(checkoutBody.address.streetNumber, "20");
      assert.equal(checkoutBody.address.postalCode, "0002");
      assert(page.url().endsWith("/checkout/pay"));
      assert.equal(errors.length, 0, errors.join("\n"));
      await noOverflow();
      await context.close();
      console.log(
        `PASS: account, address CRUD, claim, checkout optional-save failure, ${width}px`,
      );
    }
    const guest = await browser.newContext();
    const page = await guest.newPage();
    await page.goto(base + "/account/claim?token=keep-me");
    await page.waitForURL("**/login?**");
    assert.equal(
      new URL(page.url()).searchParams.get("redirect"),
      "/account/claim?token=keep-me",
    );
    await guest.close();
    console.log("PASS: signed-out claim preserves return token");
  } finally {
    await browser.close();
  }
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
