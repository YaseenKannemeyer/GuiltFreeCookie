package za.ac.cput.guiltfreecookie.domain;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PROCESSING,
    READY_FOR_PICKUP,
    SHIPPED,
    DELIVERED,
    PAYMENT_FAILED,
    CANCELLED,
    REFUNDED;

    // Money has been received and the order hasn't been refunded.
    public static final Set<OrderStatus> PAID_STATUSES = EnumSet.of(PAID, PROCESSING, READY_FOR_PICKUP, SHIPPED, DELIVERED);

    public boolean isPaid() {
        return PAID_STATUSES.contains(this);
    }
}
