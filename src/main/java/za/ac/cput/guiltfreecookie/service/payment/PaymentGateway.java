package za.ac.cput.guiltfreecookie.service.payment;

import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.dto.order.PaymentRedirect;

// How the customer is sent to pay. PayFastGateway is the only one; another provider would implement this too.
public interface PaymentGateway {

    PaymentRedirect redirectFor(CustomerOrder order);
}
