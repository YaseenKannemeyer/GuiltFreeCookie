package za.ac.cput.guiltfreecookie.dto.cart;

import za.ac.cput.guiltfreecookie.domain.DiscountType;

import java.math.BigDecimal;

// applied=false keeps the code on the cart but explains why it isn't counting (e.g. subtotal dropped below the minimum).
public record DiscountView(
        String code,
        DiscountType type,
        String description,
        BigDecimal amount,
        boolean freeShipping,
        boolean applied,
        String message) {
}
