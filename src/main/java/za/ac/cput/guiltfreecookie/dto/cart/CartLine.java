package za.ac.cput.guiltfreecookie.dto.cart;

import java.math.BigDecimal;

public record CartLine(
        String cartItemId,
        String cookieId,
        String slug,
        String name,
        String image,
        String variantId,
        String variantLabel,
        int unitsPerPack,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        int maxQuantity,
        LineIssue issue) {
}
