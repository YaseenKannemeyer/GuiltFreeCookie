package za.ac.cput.guiltfreecookie.dto.order;

import java.math.BigDecimal;

public record OrderLine(
        String cookieId,
        String name,
        String variantLabel,
        int unitsPerPack,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal) {
}
