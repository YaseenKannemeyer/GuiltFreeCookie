package za.ac.cput.guiltfreecookie.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.dto.order.OrderView;
import za.ac.cput.guiltfreecookie.service.order.OrderService;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;

/** No mapping exists outside the demo profile. Every action requires the private order token. */
@RestController
@Profile("(demo & !prod) | showcase")
@RequestMapping("/api/v1/orders/{orderNumber}/demo-payment")
public class DemoPaymentController {
    private final OrderService orders;
    private final CustomerOrderRepository repository;
    public DemoPaymentController(OrderService orders, CustomerOrderRepository repository) {
        this.orders = orders;
        this.repository = repository;
    }
    @PostMapping
    @Transactional
    public OrderView pay(@PathVariable String orderNumber, @RequestParam String token) {
        repository.findForUpdate(orderNumber).orElseThrow(() -> new NotFoundException("Order not found"));
        var order = orders.view(orderNumber, token);
        if (order.status() == OrderStatus.PAID) return order;
        orders.paymentRedirect(orderNumber, token); // rejects cancelled or expired holds
        orders.markPaid(orderNumber, "demo", "Simulated payment; no money was charged");
        return orders.view(orderNumber, token);
    }
}
