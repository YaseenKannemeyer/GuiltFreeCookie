package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.ac.cput.guiltfreecookie.dto.order.CheckoutRequest;
import za.ac.cput.guiltfreecookie.dto.order.CheckoutResponse;
import za.ac.cput.guiltfreecookie.security.CurrentUser;
import za.ac.cput.guiltfreecookie.service.order.CheckoutService;

@RestController
@RequestMapping("/api/v1/checkout")
@Tag(name = "Checkout", description = "Guest or signed-in checkout. Stock is held for 30 minutes while the customer pays.")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping
    @Operation(summary = "Place an order and get the payment redirect",
            description = "Submit payment.fields as a POST form to payment.actionUrl. Rate-limited to 20 per hour per IP.")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody CheckoutRequest request, Authentication authentication) {
        return new ResponseEntity<>(checkoutService.checkout(request, CurrentUser.customerEmail(authentication)),
                HttpStatus.CREATED);
    }
}
