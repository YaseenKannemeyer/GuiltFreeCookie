// Customer- and team-facing names for order statuses, and their badge styles.
export const ORDER_STATUS = {
  PENDING_PAYMENT: { label: "Awaiting payment", badge: "bg-chocolate/[0.07] text-chocolate" },
  PAID: { label: "Paid", badge: "bg-primary text-chocolate" },
  PROCESSING: { label: "Baking", badge: "bg-tan-light text-chocolate" },
  READY_FOR_PICKUP: { label: "Ready to collect", badge: "bg-chocolate text-cream-50" },
  SHIPPED: { label: "Shipped", badge: "bg-chocolate text-cream-50" },
  DELIVERED: { label: "Delivered", badge: "border border-chocolate/30 text-chocolate" },
  PAYMENT_FAILED: { label: "Payment failed", badge: "bg-red-800/[0.1] text-red-900" },
  CANCELLED: { label: "Cancelled", badge: "bg-red-800/[0.1] text-red-900" },
  REFUNDED: { label: "Refunded", badge: "bg-chocolate/[0.07] text-chocolate/80" },
};
export const SHIPPING_METHOD = { COURIER: "Courier", PUDO: "PUDO locker", PICKUP: "Collect" };
export const statusLabel = (status) => ORDER_STATUS[status]?.label || status;
