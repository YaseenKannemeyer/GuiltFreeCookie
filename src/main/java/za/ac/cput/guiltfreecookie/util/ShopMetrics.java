package za.ac.cput.guiltfreecookie.util;

import io.micrometer.core.instrument.Metrics;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;

import java.math.BigDecimal;

// Shop numbers for Grafana. Spring Boot adds its registries to the global one, so these need no injection
// and do nothing in plain unit tests.
public final class ShopMetrics {

    private ShopMetrics() {
    }

    // Creates every series at zero, so Grafana shows 0 instead of "No data" and counts the first event.
    public static void registerAll() {
        for (ShippingMethod method : ShippingMethod.values()) {
            Metrics.counter("gfc.orders.placed", "shipping", method.name());
            Metrics.counter("gfc.orders.paid", "shipping", method.name());
            Metrics.summary("gfc.revenue", "shipping", method.name());
        }
        for (String result : new String[]{"ok", "failed", "off"}) {
            Metrics.counter("gfc.address.searches", "result", result);
        }
        for (String result : new String[]{"live", "fallback"}) {
            Metrics.counter("gfc.courier.quotes", "result", result);
        }
        for (String result : new String[]{"booked", "refused"}) {
            Metrics.counter("gfc.courier.bookings", "result", result);
        }
        for (String result : new String[]{"sent", "failed", "expired"}) {
            Metrics.counter("gfc.emails", "result", result);
        }
    }

    public static void orderPlaced(Object shippingMethod) {
        Metrics.counter("gfc.orders.placed", "shipping", String.valueOf(shippingMethod)).increment();
    }

    public static void orderPaid(Object shippingMethod, BigDecimal total) {
        String method = String.valueOf(shippingMethod);
        Metrics.counter("gfc.orders.paid", "shipping", method).increment();
        if (total != null) {
            Metrics.summary("gfc.revenue", "shipping", method).record(total.doubleValue());
        }
    }

    // result: ok, failed or off
    public static void addressSearch(String result) {
        Metrics.counter("gfc.address.searches", "result", result).increment();
    }

    // result: live or fallback (flat rate used because ShipLogic had no answer)
    public static void courierQuote(String result) {
        Metrics.counter("gfc.courier.quotes", "result", result).increment();
    }

    // result: booked or refused
    public static void courierBooking(String result) {
        Metrics.counter("gfc.courier.bookings", "result", result).increment();
    }

    // result: sent, failed or expired
    public static void email(String result) {
        Metrics.counter("gfc.emails", "result", result).increment();
    }
}
