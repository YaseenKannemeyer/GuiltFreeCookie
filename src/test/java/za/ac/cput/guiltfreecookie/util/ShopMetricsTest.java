package za.ac.cput.guiltfreecookie.util;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeEach
    void addRegistry() {
        Metrics.addRegistry(registry);
    }

    @AfterEach
    void removeRegistry() {
        Metrics.removeRegistry(registry);
        registry.close();
    }

    @Test
    void countsOrdersRevenueAndServices() {
        ShopMetrics.orderPlaced("COURIER");
        ShopMetrics.orderPaid("COURIER", new BigDecimal("426.00"));
        ShopMetrics.orderPaid("COURIER", new BigDecimal("84.00"));
        ShopMetrics.addressSearch("ok");
        ShopMetrics.courierQuote("fallback");
        ShopMetrics.courierBooking("booked");
        ShopMetrics.email("sent");

        assertEquals(1, registry.get("gfc.orders.placed").tag("shipping", "COURIER").counter().count());
        assertEquals(2, registry.get("gfc.orders.paid").tag("shipping", "COURIER").counter().count());
        assertEquals(510.0, registry.get("gfc.revenue").summary().totalAmount());
        assertEquals(1, registry.get("gfc.address.searches").tag("result", "ok").counter().count());
        assertEquals(1, registry.get("gfc.courier.quotes").tag("result", "fallback").counter().count());
        assertEquals(1, registry.get("gfc.courier.bookings").tag("result", "booked").counter().count());
        assertEquals(1, registry.get("gfc.emails").tag("result", "sent").counter().count());
    }
}
