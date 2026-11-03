package za.ac.cput.guiltfreecookie.dto.order;

import za.ac.cput.guiltfreecookie.domain.ShippingMethod;

// address for COURIER, pudoLocker for PUDO, neither for PICKUP. email is ignored for signed-in customers.
public record CheckoutRequest(
        String cartId,
        String email,
        String firstName,
        String lastName,
        String phone,
        ShippingMethod shippingMethod,
        AddressRequest address,
        String pudoLocker,
        String note,
        Boolean newsletterOptIn) {
}
