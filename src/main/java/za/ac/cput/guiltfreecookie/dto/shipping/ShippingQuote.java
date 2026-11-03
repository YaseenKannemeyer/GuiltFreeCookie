package za.ac.cput.guiltfreecookie.dto.shipping;

import za.ac.cput.guiltfreecookie.domain.ShippingMethod;

import java.math.BigDecimal;

// freeShipping says why cost is 0: the threshold was reached or a FREE_SHIPPING code applies.
public record ShippingQuote(
        ShippingMethod method,
        String label,
        BigDecimal cost,
        String eta,
        boolean freeShipping,
        boolean requiresAddress,
        String details) {
}
