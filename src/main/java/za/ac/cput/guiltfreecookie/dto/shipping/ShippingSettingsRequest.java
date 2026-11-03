package za.ac.cput.guiltfreecookie.dto.shipping;

import java.math.BigDecimal;

public record ShippingSettingsRequest(
        Boolean courierEnabled,
        BigDecimal courierRate,
        String courierEta,
        Boolean pudoEnabled,
        BigDecimal pudoRate,
        String pudoEta,
        Boolean pickupEnabled,
        String pickupAddress,
        String pickupInstructions,
        String pickupEta,
        BigDecimal freeShippingThreshold) {
}
