package za.ac.cput.guiltfreecookie.service.payment;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.dto.order.PaymentRedirect;
import za.ac.cput.guiltfreecookie.service.order.OrderLinks;
import java.util.Map;

@Service
@Primary
@Profile("(demo & !prod) | showcase")
public class DemoPaymentGateway implements PaymentGateway {
    private final OrderLinks links;
    public DemoPaymentGateway(OrderLinks links) { this.links = links; }
    public PaymentRedirect redirectFor(CustomerOrder order) {
        return new PaymentRedirect("DEMO", "/demo-payment",
                Map.of("order", order.getOrderNumber(), "token", links.token(order)));
    }
}
