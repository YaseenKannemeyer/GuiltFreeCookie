package za.ac.cput.guiltfreecookie.dto.order;

import za.ac.cput.guiltfreecookie.domain.OrderStatus;

public record StatusChangeRequest(OrderStatus status, String note) {
}
