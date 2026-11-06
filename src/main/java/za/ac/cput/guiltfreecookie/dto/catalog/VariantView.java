package za.ac.cput.guiltfreecookie.dto.catalog;

import java.math.BigDecimal;

public record VariantView(
        String variantId,
        String label,
        int unitsPerPack,
        BigDecimal price,
        BigDecimal pricePerCookie,
        boolean available,
        // How much cheaper each cookie is than buying singles, e.g. 20 for 20%. 0 for the single itself.
        int savingPercent) {
}
