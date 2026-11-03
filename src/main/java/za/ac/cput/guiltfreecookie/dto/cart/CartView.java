package za.ac.cput.guiltfreecookie.dto.cart;

import za.ac.cput.guiltfreecookie.domain.CartStatus;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;

import java.math.BigDecimal;
import java.util.List;

public record CartView(
        String cartId,
        CartStatus status,
        String email,
        boolean accountOwned,
        List<CartLine> items,
        int itemCount,
        int cookieCount,
        BigDecimal subtotal,
        DiscountView discount,
        BigDecimal discountTotal,
        ShippingMethod shippingMethod,
        BigDecimal shipping,
        BigDecimal vatIncluded,
        BigDecimal total,
        String currency,
        boolean canCheckout) {
}
