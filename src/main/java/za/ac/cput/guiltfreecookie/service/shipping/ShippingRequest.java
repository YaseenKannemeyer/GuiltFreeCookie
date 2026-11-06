package za.ac.cput.guiltfreecookie.service.shipping;

import java.math.BigDecimal;

// merchandiseTotal is the cart subtotal after discounts. area is null until the customer has given an address.
public record ShippingRequest(BigDecimal merchandiseTotal, boolean freeShippingCode, int cookieCount, DeliveryArea area) {
}
