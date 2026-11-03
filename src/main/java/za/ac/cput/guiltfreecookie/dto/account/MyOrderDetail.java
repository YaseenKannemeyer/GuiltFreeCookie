package za.ac.cput.guiltfreecookie.dto.account;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import za.ac.cput.guiltfreecookie.dto.order.OrderView;

// The full order view at the top level, plus the token for the order page link.
public record MyOrderDetail(@JsonUnwrapped OrderView order, String token) {
}
