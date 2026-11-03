package za.ac.cput.guiltfreecookie.controller.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.dto.account.ClaimRequest;
import za.ac.cput.guiltfreecookie.dto.account.ClaimResult;
import za.ac.cput.guiltfreecookie.dto.account.ClaimableCount;
import za.ac.cput.guiltfreecookie.dto.account.MyOrderDetail;
import za.ac.cput.guiltfreecookie.dto.account.MyOrderSummary;
import za.ac.cput.guiltfreecookie.dto.catalog.PageResponse;
import za.ac.cput.guiltfreecookie.service.account.ClaimOrderService;
import za.ac.cput.guiltfreecookie.service.account.MyOrderService;

@RestController
@RequestMapping("/api/v1/me/orders")
@Tag(name = "My orders", description = "Account-owned orders and inbox-verified guest order claims")
public class MyOrderController {

    private final MyOrderService orders;
    private final ClaimOrderService claims;

    public MyOrderController(MyOrderService orders, ClaimOrderService claims) {
        this.orders = orders;
        this.claims = claims;
    }

    @GetMapping
    @Operation(summary = "My order history, newest first")
    public PageResponse<MyOrderSummary> history(
        @AuthenticationPrincipal String email,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return orders.history(email, page, size);
    }

    @GetMapping("/claimable")
    @Operation(summary = "Count earlier paid guest orders without exposing their details")
    public ClaimableCount claimable(@AuthenticationPrincipal String email) {
        return new ClaimableCount(claims.count(email));
    }

    @PostMapping("/claim")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Email a 24-hour order-claim link to my own inbox")
    public void request(@AuthenticationPrincipal String email) {
        claims.request(email);
    }

    @PostMapping("/claim/confirm")
    @Operation(summary = "Verify an inbox link and add eligible guest orders to my account")
    public ClaimResult confirm(@AuthenticationPrincipal String email, @RequestBody ClaimRequest request) {
        return new ClaimResult(claims.confirm(email, request.token()));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "My order details and tracking token; other customers' orders return 404")
    public MyOrderDetail get(@AuthenticationPrincipal String email, @PathVariable String orderNumber) {
        return orders.get(email, orderNumber);
    }
}
