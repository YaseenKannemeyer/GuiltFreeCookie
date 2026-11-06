package za.ac.cput.guiltfreecookie.service.shipping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.ac.cput.guiltfreecookie.domain.Province;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.dto.shipping.ShippingQuote;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiveShippingProviderTest {

    private static final DeliveryArea AREA = DeliveryArea.of("Rondebosch", "Cape Town", Province.WESTERN_CAPE, "7700");
    private static final BigDecimal FLAT = new BigDecimal("80.00");

    private final AtomicInteger calls = new AtomicInteger();
    private Optional<BigDecimal> liveRate = Optional.of(new BigDecimal("95"));
    private boolean enabled = true;
    private Instant now = Instant.parse("2026-11-06T08:00:00Z");
    private LiveShippingProvider provider;

    @BeforeEach
    void setUp() {
        RuleBasedShippingProvider flatRates = mock(RuleBasedShippingProvider.class);
        when(flatRates.quotes(any())).thenAnswer(call -> {
            boolean free = ((ShippingRequest) call.getArgument(0)).freeShippingCode();
            return List.of(
                    new ShippingQuote(ShippingMethod.COURIER, "Courier", free ? new BigDecimal("0.00") : FLAT, "2–4 days", free, true, null, false),
                    new ShippingQuote(ShippingMethod.PICKUP, "Collect", new BigDecimal("0.00"), "1 day", false, false, null, false));
        });
        ShipLogicClient shipLogic = new ShipLogicClient("", "http://unused", "ECO", null, null, null) {
            @Override
            public boolean enabled() {
                return enabled;
            }

            @Override
            public Optional<BigDecimal> cheapestRate(DeliveryArea area, int cookieCount, BigDecimal declaredValue) {
                calls.incrementAndGet();
                return liveRate;
            }
        };
        Clock clock = new Clock() {
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            public Instant instant() {
                return now;
            }
        };
        provider = new LiveShippingProvider(flatRates, shipLogic, clock);
    }

    private ShippingQuote courier(boolean freeCode, DeliveryArea area) {
        return provider.quotes(new ShippingRequest(new BigDecimal("300"), freeCode, 12, area)).getFirst();
    }

    @Test
    void usesTheLiveRateOnceTheAreaIsKnown() {
        ShippingQuote quote = courier(false, AREA);
        assertEquals(new BigDecimal("95.00"), quote.cost());
        assertFalse(quote.estimate());
    }

    @Test
    void withoutAnAreaTheFlatRateIsAnEstimate() {
        ShippingQuote quote = courier(false, null);
        assertEquals(FLAT, quote.cost());
        assertTrue(quote.estimate());
        assertEquals(0, calls.get());
    }

    @Test
    void fallsBackToTheFlatRateWhenShipLogicFails() {
        liveRate = Optional.empty();
        ShippingQuote quote = courier(false, AREA);
        assertEquals(FLAT, quote.cost());
        assertFalse(quote.estimate());
    }

    @Test
    void freeDeliveryAndOtherMethodsNeverCallShipLogic() {
        assertEquals(new BigDecimal("0.00"), courier(true, AREA).cost());
        assertEquals(new BigDecimal("0.00"), provider.quotes(new ShippingRequest(BigDecimal.TEN, false, 1, AREA)).getLast().cost());
        assertEquals(1, calls.get());
    }

    @Test
    void switchedOffKeepsTheFlatRateWithoutAnEstimate() {
        enabled = false;
        ShippingQuote quote = courier(false, null);
        assertEquals(FLAT, quote.cost());
        assertFalse(quote.estimate());
        assertEquals(0, calls.get());
    }

    @Test
    void cachesEachAreaForTenMinutes() {
        courier(false, AREA);
        now = now.plusSeconds(599);
        courier(false, DeliveryArea.of("rondebosch", "cape town", Province.WESTERN_CAPE, "7700"));
        assertEquals(1, calls.get());
        now = now.plusSeconds(2);
        courier(false, AREA);
        assertEquals(2, calls.get());
    }
}
