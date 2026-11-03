package za.ac.cput.guiltfreecookie.dto.account;

import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MyOrderSummary(
        String orderNumber,
        LocalDateTime createdAt,
        OrderStatus status,
        ShippingMethod shippingMethod,
        int itemCount,
        BigDecimal total,
        String image,
        String token) {
}
