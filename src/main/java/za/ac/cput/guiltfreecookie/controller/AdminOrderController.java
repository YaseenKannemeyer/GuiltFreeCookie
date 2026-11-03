package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.dto.catalog.PageResponse;
import za.ac.cput.guiltfreecookie.dto.order.AdminOrderView;
import za.ac.cput.guiltfreecookie.dto.order.NoteRequest;
import za.ac.cput.guiltfreecookie.dto.order.OrderSummary;
import za.ac.cput.guiltfreecookie.dto.order.StatusChangeRequest;
import za.ac.cput.guiltfreecookie.dto.order.TrackingRequest;
import za.ac.cput.guiltfreecookie.service.order.AdminOrderService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/orders")
@Tag(name = "Admin orders", description = "Find, fulfil, ship and refund orders. Admin token required.")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    @Operation(summary = "Search orders, newest first", description = "q matches order number, email or last name. Dates are yyyy-MM-dd, inclusive.")
    public ResponseEntity<PageResponse<OrderSummary>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(adminOrderService.search(q, status, from, to, page, size));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<AdminOrderView> get(@PathVariable String orderNumber) {
        return ResponseEntity.ok(adminOrderService.get(orderNumber));
    }

    @PutMapping("/{orderNumber}/status")
    @Operation(summary = "Move an order on",
            description = "PAID→PROCESSING, PROCESSING→READY_FOR_PICKUP (pickup orders, emails the customer), "
                    + "READY_FOR_PICKUP or SHIPPED→DELIVERED, PENDING_PAYMENT→CANCELLED (frees the stock).")
    public ResponseEntity<AdminOrderView> changeStatus(@PathVariable String orderNumber,
                                                       @RequestBody StatusChangeRequest request,
                                                       @AuthenticationPrincipal String adminId) {
        return ResponseEntity.ok(adminOrderService.changeStatus(orderNumber, request, adminId));
    }

    @PutMapping("/{orderNumber}/tracking")
    @Operation(summary = "Ship with tracking", description = "Marks a courier or PUDO order shipped and emails the customer. Call again to correct the details.")
    public ResponseEntity<AdminOrderView> ship(@PathVariable String orderNumber, @RequestBody TrackingRequest request,
                                               @AuthenticationPrincipal String adminId) {
        return ResponseEntity.ok(adminOrderService.ship(orderNumber, request, adminId));
    }

    @PostMapping("/{orderNumber}/refund")
    @Operation(summary = "Record a refund", description = "Restocks if the cookies hadn't left yet. Pay the money back in the PayFast dashboard.")
    public ResponseEntity<AdminOrderView> refund(@PathVariable String orderNumber, @RequestBody(required = false) NoteRequest request,
                                                 @AuthenticationPrincipal String adminId) {
        return ResponseEntity.ok(adminOrderService.refund(orderNumber, request == null ? null : request.note(), adminId));
    }
}
