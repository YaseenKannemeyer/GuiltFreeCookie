package za.ac.cput.guiltfreecookie.dto.catalog;

import java.math.BigDecimal;

public record VariantView(
        String variantId,
        String label,
        int unitsPerPack,
        BigDecimal price,
        BigDecimal pricePerCookie,
        boolean available) {
}
