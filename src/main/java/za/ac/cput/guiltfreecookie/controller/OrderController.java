package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.dto.order.OrderView;
import za.ac.cput.guiltfreecookie.dto.order.PaymentRedirect;
import za.ac.cput.guiltfreecookie.service.order.OrderService;

// token is the one from checkout or the order email link; it's the guest's key to the order.
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Thank-you and tracking page for an order, by order number and token.")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Order status, items, totals and timeline")
    public ResponseEntity<OrderView> get(@PathVariable String orderNumber, @RequestParam String token) {
        return ResponseEntity.ok(orderService.view(orderNumber, token));
    }

    @PostMapping("/{orderNumber}/payment")
    @Operation(summary = "Try paying again", description = "Only while the order is unpaid and its stock is still held.")
    public ResponseEntity<PaymentRedirect> payAgain(@PathVariable String orderNumber, @RequestParam String token) {
        return ResponseEntity.ok(orderService.paymentRedirect(orderNumber, token));
    }

    @PostMapping("/{orderNumber}/cancel")
    @Operation(summary = "Cancel an unpaid order", description = "Call from PayFast's cancel page: frees the stock and reopens the cart.")
    public ResponseEntity<OrderView> cancel(@PathVariable String orderNumber, @RequestParam String token) {
        return ResponseEntity.ok(orderService.cancelUnpaid(orderNumber, token));
    }
}
