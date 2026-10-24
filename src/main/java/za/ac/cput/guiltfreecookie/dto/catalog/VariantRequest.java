package za.ac.cput.guiltfreecookie.dto.catalog;

import java.math.BigDecimal;

// price null = unit price x unitsPerPack.
public record VariantRequest(String label, Integer unitsPerPack, BigDecimal price, Boolean active, Integer sortOrder) {
}
