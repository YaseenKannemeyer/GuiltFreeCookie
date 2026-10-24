package za.ac.cput.guiltfreecookie.dto.order;

import za.ac.cput.guiltfreecookie.domain.OrderStatus;

import java.math.BigDecimal;

// accessToken unlocks the order page for guests; keep it with the order number. payment is null when nothing is owed.
public record CheckoutResponse(
        String orderNumber,
        String accessToken,
        OrderStatus status,
        BigDecimal total,
        PaymentRedirect payment) {
}
