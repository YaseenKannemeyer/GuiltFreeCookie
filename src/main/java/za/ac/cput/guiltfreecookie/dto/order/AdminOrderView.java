package za.ac.cput.guiltfreecookie.dto.order;

import za.ac.cput.guiltfreecookie.domain.OrderStatusEvent;
import za.ac.cput.guiltfreecookie.domain.Payment;

import java.util.List;

// The customer view plus who changed what, the customer's note, and every payment notification.
// courierBooking says the order can be booked with The Courier Guy from the admin.
public record AdminOrderView(OrderView order, String customerNote, List<OrderStatusEvent> events, List<Payment> payments,
                             boolean courierBooking) {
}
