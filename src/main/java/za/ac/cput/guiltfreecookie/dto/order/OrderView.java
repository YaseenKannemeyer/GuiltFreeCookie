package za.ac.cput.guiltfreecookie.dto.order;

import za.ac.cput.guiltfreecookie.domain.Address;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// What a customer sees on the thank-you and tracking page.
public record OrderView(
        String orderNumber,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        String email,
        String firstName,
        String lastName,
        String phone,
        ShippingMethod shippingMethod,
        Address address,
        String pudoLocker,
        String pickupDetails,
        String deliveryEta,
        List<OrderLine> items,
        BigDecimal subtotal,
        String discountCode,
        BigDecimal discountTotal,
        BigDecimal shippingTotal,
        BigDecimal vatIncluded,
        BigDecimal total,
        String courierName,
        String trackingNumber,
        String trackingUrl,
        List<TimelineEntry> timeline,
        LocalDateTime payBy,
        boolean canPay,
        boolean canCancel,
        // Only while the order is unpaid or was cancelled unpaid: lets the browser get its cart back.
        String cartId) {
}
