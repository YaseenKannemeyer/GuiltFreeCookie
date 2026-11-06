package za.ac.cput.guiltfreecookie.service.cart;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PricingServiceTest {

    private PricingService pricing(boolean vatRegistered) {
        return new PricingService(null, null, null, null, vatRegistered, new BigDecimal("0.15"), null);
    }

    @Test
    void vatIsTheIncludedPortionOfAVatInclusiveTotal() {
        assertEquals(new BigDecimal("15.00"), pricing(true).vatIncluded(new BigDecimal("115.00")));
        assertEquals(new BigDecimal("26.09"), pricing(true).vatIncluded(new BigDecimal("200.00")));
    }

    @Test
    void noVatLineWhenNotVatRegistered() {
        assertNull(pricing(false).vatIncluded(new BigDecimal("115.00")));
    }
}
