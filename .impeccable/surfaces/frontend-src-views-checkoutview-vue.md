---
version: 1
slug: "frontend-src-views-checkoutview-vue"
primary_target: "frontend/src/views/CheckoutView.vue"
related_targets: ["frontend/src/views/CartView.vue","frontend/src/views/OrderView.vue","frontend/src/views/AdminOrdersView.vue"]
---

# Surface brief: purchase flow (cart, checkout, order tracking, payment cancelled) + admin orders & delivery

Scope and mode: Operate. Shoppers (personal treats and gift/bulk buyers, mostly on phones) go from cart to paid order. The small team runs orders and delivery settings on phones and laptops.

Task and states:
- Cart: review items and allergens, fix stock issues, apply a code.
- Checkout (You → Delivery → Pay): contact details, delivery choice with price and ETA, then a full review with the ECTA s43 disclosures before handing off to PayFast.
- Order page: polls while waiting for PayFast's notification, then tracks the order.
- Payment cancelled: frees the stock and returns the shopper to their cart.
- Admin: orders (search, next-status actions, tracking, refunds) and delivery settings.

Constraints: no health claims (R146); allergens visible in cart and checkout; delivery times are estimates; the total shown before paying is the total charged.

## Direction contract

THESIS: One rail from cart to doorstep. The four checkout pills (Cart, You, Delivery, Pay) continue on the order page as the tracking rail (Paid, Baking, On its way / Ready to collect, Delivered). This refuses the category's two-column store checkout with a summary sidebar.

OWN-WORLD: The incumbent world: a cream #E3D9C8 ground with #F5F0E6 tiles, chocolate #2E0E0B ink and primary pills, and caramel #C59F71 for the current step and the chosen option. Luckiest Guy is used only for the single question on each screen; Nunito is used everywhere else. Rand amounts use tabular figures on one right edge.

STORY: The shopper answers one question at a time, sees exactly what they'll pay including delivery before PayFast, and afterwards watches the same rail advance as the bakery works.

FIRST VIEWPORT: On a phone, the step rail sits on top; one Luckiest Guy question (~40px); the tiles or fields for that step; and a sticky bottom bar with "Total incl. delivery" on the left above a full-width chocolate Continue pill. On desktop, it's a centered 40rem column with the same rail, and the bar is an inline footer.

FORM: Step-by-step wizard, position 6 on my ordered list, seed key 9af205d8. Signature interaction: the chosen tile floods caramel-tinted with a 2px chocolate edge (kept from the declined Metro challenger), totals update in place, and the rail fills forward over 200ms. Order-pass segments (competitive challenger) shape the order page header.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
