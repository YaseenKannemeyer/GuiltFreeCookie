package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.dto.order.OrderView;
import za.ac.cput.guiltfreecookie.dto.order.PaymentRedirect;
import za.ac.cput.guiltfreecookie.service.order.OrderDocuments;
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

    @GetMapping(value = "/{orderNumber}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download the A4 PDF invoice", description = "Once the order is paid; 409 before that.")
    public ResponseEntity<byte[]> invoice(@PathVariable String orderNumber, @RequestParam String token) {
        return pdf(orderService.document(orderNumber, token, OrderDocuments.Kind.INVOICE));
    }

    @GetMapping(value = "/{orderNumber}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download the till-slip PDF receipt", description = "Once the order is paid; 409 before that.")
    public ResponseEntity<byte[]> receipt(@PathVariable String orderNumber, @RequestParam String token) {
        return pdf(orderService.document(orderNumber, token, OrderDocuments.Kind.RECEIPT));
    }

    @PostMapping("/{orderNumber}/payment")
    @Operation(summary = "Try paying again", description = "Only while the order is unpaid and its stock is still held.")
    public ResponseEntity<PaymentRedirect> payAgain(@PathVariable String orderNumber, @RequestParam String token) {
        return ResponseEntity.ok(orderService.paymentRedirect(orderNumber, token));
    }

    @PostMapping("/{orderNumber}/cancel-paid")
    @Operation(summary = "Cancel a paid order before baking starts",
            description = "Only while the order is PAID (409 once the bakery has accepted it). Marks it refunded and emails the shop to pay the refund.")
    public ResponseEntity<OrderView> cancelPaid(@PathVariable String orderNumber, @RequestParam String token) {
        return ResponseEntity.ok(orderService.cancelPaid(orderNumber, token));
    }

    @PostMapping("/{orderNumber}/cancel")
    @Operation(summary = "Cancel an unpaid order", description = "Call from PayFast's cancel page: frees the stock and reopens the cart.")
    public ResponseEntity<OrderView> cancel(@PathVariable String orderNumber, @RequestParam String token) {
        return ResponseEntity.ok(orderService.cancelUnpaid(orderNumber, token));
    }

    private static ResponseEntity<byte[]> pdf(OrderService.Document document) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.fileName()).build().toString())
                .body(document.pdf());
    }
}
