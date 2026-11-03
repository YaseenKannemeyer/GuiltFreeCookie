package za.ac.cput.guiltfreecookie.dto.order;

import za.ac.cput.guiltfreecookie.domain.OrderStatus;

import java.time.LocalDateTime;

public record TimelineEntry(OrderStatus status, String note, LocalDateTime at) {
}
