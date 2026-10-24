package za.ac.cput.guiltfreecookie.dto.order;

import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummary(
        String orderNumber,
        LocalDateTime createdAt,
        String customerName,
        String email,
        ShippingMethod shippingMethod,
        int itemCount,
        BigDecimal total,
        OrderStatus status) {
}
