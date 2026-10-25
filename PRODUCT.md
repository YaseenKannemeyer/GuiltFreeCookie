# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Shoppers (two equal groups):**
  - People ordering a treat for themselves who are cutting sugar, managing blood sugar, eating high-protein, or avoiding gluten or animal products. They read ingredients and allergens before buying.
  - Gift and event buyers ordering boxes for presents, offices or functions. They care about delivery timing, presentation and quantity.
  - Most traffic is mobile.
- **Shop team:** a small team handles orders, stock and delivery settings on phones and laptops, often between bakes.

## Product Purpose

GuiltFreeCookie is an online shop for a small-batch bakery in Strandfontein, Cape Town. It sells cookies made with less sugar or for specific diets, in single cookies and boxes (pack sizes), for courier delivery, PUDO locker collection or pickup at the bakery. Success means a shopper can choose with confidence, pay without friction, and always know where their order is. The team can then fulfil orders without confusion.

## Positioning

A local bakery that bakes to order and is specific about what's in each cookie, with categories such as low sugar, high protein, gluten free and vegan. It is not a generic cookie shop with a "healthy" sticker.

## Operating Context

- **Guest-first:** an account is never required. Signed-in customers get a saved cart and order history.
- **Checkout:** prices are VAT-inclusive rand. Delivery is chosen at checkout (courier, PUDO, or pickup); the defaults are courier R80 and PUDO R60, both free over R500, and pickup is free. Stock is held for 30 minutes while the customer pays.
- **Payment:** PayFast's hosted page (cards, Instant EFT, SnapScan, wallets, 3-D Secure). Card details never touch the site.
- **Order statuses and links:** orders move pending payment → paid → processing → ready for pickup or shipped → delivered. Customers follow their order through a private link (order number + token) emailed to them.
- **Admins** manage stock, pack sizes, discount codes, orders (status, tracking, refunds recorded here and paid back in the PayFast dashboard) and delivery settings.

## Capabilities and Constraints

- **Fresh, baked to order:** delivery times are honest estimates, never guarantees.
- **No medical claims (R146):** R146 covers website advertising, and claims must not mislead. Never state or imply that a cookie treats, prevents or manages a disease, including diabetes. Category names (e.g. "Diabetic friendly") are labels only, never claims of safety or suitability. Open decision: whether "Diabetic friendly" should be renamed after legal review.
- **Allergens always visible:** allergens must be visible before purchase, and must be reachable from the cart and checkout as well as the product page.
- **ECTA s43 disclosures:** before payment the shopper must see the full price including delivery, delivery estimates, the accepted payment methods, the refund/return policy and terms, and the shop's contact details.
- **ECTA s44 cooling-off:** the 7-day cooling-off right excludes foodstuffs for everyday consumption delivered to a home or workplace (s42(2)(c)). Don't promise a cooling-off period. Returns follow the published refund policy.
- **POPIA:** marketing email needs opt-in consent and every marketing email carries an unsubscribe link.
- **API:** the contract is `docs/API-CHANGELOG.md`, and request DTOs use boxed types.

## Brand Commitments

- The name is "GuiltFreeCookie"; the wordmark is "GuiltFree" + "Cookie".
- The existing frontend and branded emails define the visual identity. They are the incumbent authority until a DESIGN.md exists.
- Voice: warm, plain, a little playful, never preachy about diet.

## Evidence on Hand

- **Product photography:** transparent WebP cookies in `frontend/public/img/cookies/` and banners in `frontend/public/img/banners/`.
- **Catalogue:** 12 imported cookies (see `docs/cookie-catalog-handoff.md`). Recipes, allergens and prices still need the owner's confirmation; nutrition values must not be invented.
- **Reviews:** there are no real reviews or testimonials yet. Ratings show only from approved customer reviews, and none may be fabricated.
- **Legal pages:** privacy, terms, refund and cookie policy pages exist in the frontend.

## Product Principles

1. **Honest over persuasive:** real stock, real delivery estimates and real reviews, with no health claims.
2. **Never block a purchase on an account:** guests get the full experience.
3. **The total is the total:** what the shopper sees before paying is exactly what is charged.
4. **Always know the next step:** every order state tells the shopper or the team what happens next.
5. **Built for one hand on a phone,** including for the team in the kitchen.
