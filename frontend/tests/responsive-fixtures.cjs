// Illustrative data only. Browser tests never contact the live API.
const fs = require('node:fs');
const path = require('node:path');
const customer = { customerEmail: 'sam.baker@example.com', name: { firstName: 'Sam', lastName: 'Baker' }, mobileNumber: '0821234567' };
const admin = { adminId: 'a1', firstName: 'Alexandra', lastName: 'Baker', email: 'alexandra.baker@example.com', active: true };
const cookie = {
  cookieId: 'c1', slug: 'double-chocolate-chunk', name: 'Double Chocolate Chunk', description: 'Double Chocolate Chunk',
  category: 'HIGH_PROTEIN', image: 'cookies/double-chocolate-chunk.webp', fromPrice: 34, price: 34,
  featured: true, stockStatus: 'LOW_STOCK', stockQuantity: 8, lowStockThreshold: 12, lowStockRemaining: 8,
  longDescription: 'Deep cocoa and dark chocolate chunks with a soft centre.', ingredients: 'Almond flour, cocoa, dark chocolate', allergies: 'Almonds',
  nutrition: { calories: 160, proteinGrams: 6 }, averageRating: 5, reviewCount: 1,
};
const variant = { variantId: 'v1', label: 'Single', unitsPerPack: 1, price: 34, pricePerCookie: 34, available: true, active: true, sortOrder: 0 };
const images = [{ imageId: 'i1', image: cookie.image, altText: 'Chocolate cookie close up' }];
const item = { cartItemId: 'line1', cookieId: cookie.cookieId, slug: cookie.slug, name: cookie.name, image: cookie.image,
  variantLabel: 'Single', unitsPerPack: 1, quantity: 2, maxQuantity: 10, lineTotal: 68, allergens: 'Almonds' };
const cart = { cartId: 'responsive-cart', status: 'ACTIVE', itemCount: 2, cookieCount: 2, subtotal: 68, total: 68, discountTotal: 0, canCheckout: true, items: [item] };
const address = { streetNumber: '12', streetName: 'Main Road', suburb: 'Rondebosch', city: 'Cape Town', province: 'WESTERN_CAPE', postalCode: '7700' };
const order = { ...cart, orderNumber: 'GFC-20261106-1042', status: 'DELIVERED', firstName: 'Sam', lastName: 'Baker', email: customer.customerEmail,
  customerName: 'Sam Baker', phone: customer.mobileNumber, shippingMethod: 'COURIER', shippingTotal: 80, total: 148, address,
  createdAt: '2026-11-06T10:00:00', timeline: [{ at: '2026-11-06T10:00:00', note: 'Order delivered' }], deliveryEta: '2–3 working days' };
const review = { id: 'r1', cookieId: 'c1', rating: 5, title: 'A lovely afternoon treat', body: 'Full of chocolate flavour and just the right amount of crunch.', authorName: 'Sam', status: 'PENDING', createdAt: order.createdAt };
const adminCookie = { cookie, variants: [variant], images, stockStatus: 'LOW_STOCK' };
const shipping = { courierEnabled: true, courierRate: 80, courierEta: '2–3 working days', pudoEnabled: true, pudoRate: 60, pudoEta: '3–5 working days', pickupEnabled: true, pickupAddress: '12 Bakery Road, Cape Town', freeShippingThreshold: 500 };
const pageOf = items => ({ items, page: 0, totalPages: 2, totalItems: 14 });
const options = [{ method: 'COURIER', label: 'Courier to your door', cost: 80, eta: '2–3 working days' }, { method: 'PUDO', label: 'PUDO locker', cost: 60, eta: '3–5 working days' }, { method: 'PICKUP', label: 'Collect from the bakery', cost: 0, eta: 'Ready tomorrow' }];
function seed({ customer, admin, address }) {
  if (window !== window.top) return;
  localStorage.setItem('gfc_admin_session', JSON.stringify({ admin, token: 'fixture-admin-token' }));
  sessionStorage.setItem('gfc_customer_session', JSON.stringify({ customer, token: 'fixture-customer-token' }));
  localStorage.setItem('gfc_cart_id', 'responsive-cart');
  sessionStorage.setItem('gfc_checkout_draft', JSON.stringify({ firstName: 'Sam', lastName: 'Baker', email: customer.customerEmail, phone: customer.mobileNumber, shippingMethod: 'COURIER', address }));
}
async function mockApi(route) {
  const url = new URL(route.request().url());
  const p = url.pathname.replace('/guiltFreeCookie', '').replace(/^\/api\/v1/, '');
  if (p.startsWith('/uploads/')) return route.fulfill({ contentType: 'image/webp', body: fs.readFileSync(path.join(__dirname, '../public/img/cookies/double-chocolate-chunk.webp')) });
  let data;
  if (p === '/site/info') data = { supportEmail: 'hello@guiltfreecookie.co.za', phone: '0821234567', address: 'Cape Town, South Africa', social: {}, stats: { reviewCount: 1, averageRating: 5 } };
  else if (p === '/cookies/categories') data = [{ category: 'HIGH_PROTEIN', label: 'High protein', count: 14 }];
  else if (p === '/cookies') data = pageOf([cookie, { ...cookie, cookieId: 'c2', name: 'Lemon Almond', slug: 'lemon-almond' }]);
  else if (p === '/reviews/featured') data = [review];
  else if (p.startsWith('/cookies/') && p.endsWith('/reviews')) data = { summary: { averageRating: 5, reviewCount: 1, histogram: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 1 } }, page: pageOf([review]) };
  else if (p.startsWith('/cookies/')) data = { ...cookie, variants: [variant], images, related: [{ ...cookie, cookieId: 'c2', name: 'Lemon Almond' }] };
  else if (p.endsWith('/shipping-options')) data = options;
  else if (p.startsWith('/carts') || p === '/me/cart/merge') data = cart;
  else if (p.startsWith('/orders/') && p.endsWith('/reorder')) data = cart;
  else if (p.startsWith('/orders/')) data = order;
  else if (p === '/me') data = customer;
  else if (p === '/me/addresses') data = [{ id: 'address1', label: 'Home', recipientName: 'Sam Baker', phone: customer.mobileNumber, isDefault: true, address }];
  else if (p === '/me/orders/claimable') data = { count: 1 };
  else if (p === '/me/orders/claim/confirm') data = { claimed: 1 };
  else if (p === '/me/orders') data = pageOf([order]);
  else if (p === '/admin/cookies' || p === '/admin/inventory/low-stock') data = [adminCookie];
  else if (p.endsWith('/inventory')) data = [{ quantityChange: 8, stockAfter: 8, reason: 'RESTOCK', note: 'Fresh batch', actor: 'a1', createdAt: order.createdAt }];
  else if (p.startsWith('/admin/cookies/')) data = adminCookie;
  else if (p === '/cookie/getAllForAdmin' || p === '/cookie/getAll') data = [cookie];
  else if (p === '/admin/getAll') data = [admin, { ...admin, adminId: 'a2', firstName: 'Thandiwe', email: 'thandiwe.mokoena@example.com' }];
  else if (p === '/admin/orders') data = pageOf([order]);
  else if (p.startsWith('/admin/orders/')) data = { order: { ...order, status: 'PROCESSING' }, events: [{ eventId: 'e1', status: 'PAID', createdAt: order.createdAt, actor: 'system' }], payments: [], courierBooking: true };
  else if (p === '/admin/shipping-settings') data = shipping;
  else if (p === '/admin/messages') data = [{ id: 'm1', name: 'Sam', email: customer.customerEmail, subject: 'Cookies for an office celebration', message: 'Could I order cookies for our office?', status: 'NEW', createdAt: order.createdAt }];
  else if (p === '/admin/reviews') data = [review];
  else if (p === '/admin/email-previews') data = ['order-confirmation'];
  else if (p.startsWith('/admin/email-previews/')) return route.fulfill({ contentType: 'text/html', body: '<h1>Sample order confirmation</h1>' });
  else if (p === '/admin/email-deliveries') data = [{ id: 'd1', subject: 'Your order is confirmed', recipient: customer.customerEmail, status: 'FAILED', attempts: 1, canRetry: true }];
  else if (p === '/admin/email-flows/stats') data = [];
  else if (p === '/admin/email-flows') data = { welcomeEnabled: true, welcomeDiscountPercent: 10, welcomeCodeDays: 7 };
  else if (p === '/admin/discounts') data = [{ discountId: 'd1', code: 'WELCOME10', type: 'PERCENT', value: 10, active: true, usedCount: 2 }];
  else if (p === '/admin/analytics') data = {
    current: { revenue: 12500, orders: 25, averageOrderValue: 500, cookiesSold: 300, discounts: 50 },
    previous: { revenue: 10000, orders: 20, averageOrderValue: 500, cookiesSold: 240 },
    days: [{ date: '2026-11-05', revenue: 500, orders: 1 }, { date: '2026-11-06', revenue: 1000, orders: 2 }],
    topCookies: [{ cookieId: 'c1', name: cookie.name, revenue: 12500, cookies: 300, packs: 50 }],
    customers: { newCustomers: 10, returningCustomers: 15 }, funnel: { carts: 40, cartsWithEmail: 30, ordersPlaced: 28, ordersPaid: 25 },
    delivery: [{ method: 'COURIER', orders: 25 }], emailFlows: [{ flow: 'WELCOME', sent: 10, opened: 8, clicked: 4, orders: 2, revenue: 1000 }],
  };
  else if (p === '/address-search') data = { enabled: true, suggestions: url.searchParams.get('q') ? [{ placeId: 'place1', mainText: '12 Main Road', secondaryText: 'Rondebosch, Cape Town' }] : [] };
  else if (p === '/address-search/place') data = address;
  else if (p.startsWith('/email/') || p === '/events/product-view') data = {};
  else throw new Error(`Missing responsive fixture: ${route.request().method()} ${p}`);
  await route.fulfill({ json: data });
}
module.exports = { mockApi, seed, seedData: { customer, admin, address }, cookie };
