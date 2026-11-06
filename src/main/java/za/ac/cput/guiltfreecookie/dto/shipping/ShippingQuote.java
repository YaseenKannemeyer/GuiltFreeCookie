package za.ac.cput.guiltfreecookie.dto.shipping;

import za.ac.cput.guiltfreecookie.domain.ShippingMethod;

import java.math.BigDecimal;

// freeShipping says why cost is 0: the threshold was reached or a FREE_SHIPPING code applies.
// estimate means the cost may change once the delivery address is known.
public record ShippingQuote(
        ShippingMethod method,
        String label,
        BigDecimal cost,
        String eta,
        boolean freeShipping,
        boolean requiresAddress,
        String details,
        boolean estimate) {

    public ShippingQuote withCost(BigDecimal newCost, boolean newEstimate) {
        return new ShippingQuote(method, label, newCost, eta, freeShipping, requiresAddress, details, newEstimate);
    }
}
