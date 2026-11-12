# Smoke test

A quick hand check that the shop's basics work: run it on **staging** after every merge to `staging`, and a shorter pass on **production** after every release (see [WORKFLOW.md](WORKFLOW.md)). It takes about 20 minutes on staging and 5 on production.

Tick each box as you go. If one fails, stop, note what you saw (page, time, order number) and check Sentry for the error.

## Before you start

| | Staging | Production |
|---|---|---|
| Storefront | the `staging` branch preview on Vercel | [guiltfreecookie.me](https://guiltfreecookie.me) |
| API health | `https://guiltfreecookie-api-staging.azurewebsites.net/guiltFreeCookie/actuator/health` | `https://guiltfreecookie-api.azurewebsites.net/guiltFreeCookie/actuator/health` |
| Payments | PayFast **sandbox**: no real money | PayFast **sandbox**: no real money |
| Courier | ShipLogic **sandbox**: test bookings | ShipLogic **sandbox**: test bookings |
| Emails | **Captured**: they don't leave the server. Read them in **Admin → Email previews → Demo inbox** | **Real**: sent through Brevo from `hello@guiltfreecookie.me` to the address you enter |

- [ ] API health says `"status":"UP"` (if not, wait: it takes about 5 minutes after a deploy)
- [ ] Note the stock of one cookie in **Admin → Inventory** before you start (you'll check it changes)

Use your own email address for test orders. On production, remember that test orders and accounts stay in the real database until it's cleaned (TODO, "Clean the production database").

## 1. Storefront

- [ ] Home page loads, with images and no error message
- [ ] **Shop** lists the cookies with prices; out-of-stock cookies are marked
- [ ] A cookie's page opens, shows pack sizes and prices, and **Add to cart** works
- [ ] **About**, **Contact** and the legal pages (privacy, terms) open
- [ ] On a phone (or a narrow browser window) the menu opens and the pages fit the screen

## 2. Cart

- [ ] The cart count in the header goes up when you add a cookie
- [ ] In the cart, change a quantity: the total updates
- [ ] Remove an item: it disappears and the total updates
- [ ] Reload the page: the cart is still there
- [ ] A discount code is accepted and the discount shows (staging: `DEMO10` takes 10% off); a made-up code is refused with a message

## 3. Checkout and payment (courier delivery)

- [ ] At checkout, **Search your address** suggests real South African addresses as you type, and picking one fills in street, suburb, city, province and postal code
- [ ] The courier price appears for that area (marked as an estimate if the courier didn't answer)
- [ ] Free delivery applies from R750
- [ ] **Pay** goes to the PayFast **sandbox** page with the right total
- [ ] Complete the sandbox payment: you return to the shop's order page showing the order as **Paid**
- [ ] Note the order number (`GFC-…`)

## 4. Stock

- [ ] The cookie you bought has gone down by the quantity ordered in **Admin → Inventory**
- [ ] Start another checkout and **cancel** on the PayFast page: you land on "Payment cancelled", the order is **Cancelled** (or still **Pending payment**), and its stock comes back (immediately on cancel, or within 30 minutes when an unpaid checkout expires)
- [ ] In **Admin → Inventory**, change a cookie's stock: the new number shows on the storefront after a reload
- [ ] Set a cookie's stock to 0: the storefront shows it as out of stock and won't let you add it. Put the stock back afterwards (or run `scripts/restock.sh staging`)

## 5. Emails

Staging: **Admin → Email previews → Demo inbox**. Production: your real inbox (check spam too).

- [ ] **Order confirmation** arrived for the paid order, with the right items, total and delivery address
- [ ] Links and images in the email work
- [ ] The contact form (**Contact** page) sends, and the message appears in **Admin → Messages**
- [ ] Production only: the email is from `hello@guiltfreecookie.me` and isn't in spam

## 6. Admin: orders and delivery

Log in at `/admin`.

- [ ] **Overview / Dashboard** loads with today's numbers, including the test order
- [ ] **Orders** lists the test order; opening it shows items, customer, address and payment
- [ ] **Book courier** on the paid courier order books a test shipment (only Monday–Wednesday on staging; production allows every day for demos) and the order moves to **Shipped** with a tracking number
- [ ] **Download waybill** opens the courier's PDF label
- [ ] The **Shipped** email arrives with the tracking link
- [ ] Move another paid order through its statuses (Processing → Shipped or Ready for pickup → Delivered): each change saves, and the customer email for it arrives
- [ ] **Delivery** settings load; the delivery options shown match what checkout offers (PUDO is off)
- [ ] **Discounts**, **Reviews**, **Email flows** and **Users → Admins** pages load without errors

## 7. Customer accounts

- [ ] **Create account** at `/register` with a new email: it signs you in
- [ ] Log out, then **Log in** at `/login` with the same details
- [ ] Place an order while logged in: it appears under **My account → Orders**
- [ ] Save an address in **My account** and see it offered at the next checkout
- [ ] **Forgot password** at `/forgot-password`: the reset email arrives (staging: Demo inbox), the link sets a new password (it works for 60 minutes), and the new password logs in
- [ ] A wrong password is refused with a message (not an error page)
- [ ] Known gap: **Admin → Users → Customers** says "Customer management isn't available here yet". The API already has admin-only customer endpoints (`/api/v1/customers`); only the admin screen is missing

## 8. Monitoring

- [ ] [status.guiltfreecookie.me](https://status.guiltfreecookie.me) is all green (production)
- [ ] Sentry shows no new issues from your test, for the environment you tested
- [ ] Grafana dashboard (Environment set to the one you tested): the test order shows in **Orders placed** and **Orders paid**, the courier quote and booking in the Courier row, and the address searches; no spike in **Errors (5xx)**

## Production: the short pass after a release

- [ ] API health `UP` and the status page green
- [ ] Home, Shop and one cookie page load on `guiltfreecookie.me`
- [ ] Add to cart, address search and courier price at checkout
- [ ] One sandbox payment goes through and the confirmation email arrives in your real inbox
- [ ] Stock went down in **Admin → Inventory**
- [ ] No new Sentry issues for `production`; Grafana shows the order

## Record of runs

| Date | Environment | Who | Result | Notes |
|---|---|---|---|---|
| | | | | |
