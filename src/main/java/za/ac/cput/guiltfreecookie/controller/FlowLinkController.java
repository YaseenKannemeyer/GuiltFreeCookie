package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.dto.cart.CartView;
import za.ac.cput.guiltfreecookie.dto.engagement.ReviewView;
import za.ac.cput.guiltfreecookie.dto.flow.LinkedReviewRequest;
import za.ac.cput.guiltfreecookie.dto.flow.ProductViewEvent;
import za.ac.cput.guiltfreecookie.security.CurrentUser;
import za.ac.cput.guiltfreecookie.service.flow.EmailLinkService;
import za.ac.cput.guiltfreecookie.service.flow.ProductViewService;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Email flow links", description = "Cart recovery, reorder, review-by-link and the product-view signal.")
public class FlowLinkController {

    private final EmailLinkService linkService;
    private final ProductViewService productViewService;

    public FlowLinkController(EmailLinkService linkService, ProductViewService productViewService) {
        this.linkService = linkService;
        this.productViewService = productViewService;
    }

    @GetMapping("/carts/recover/{token}")
    @Operation(summary = "Reopen a saved guest cart from a reminder email", description = "Store the returned cartId.")
    public ResponseEntity<CartView> recover(@PathVariable String token) {
        return ResponseEntity.ok(linkService.recoverCart(token));
    }

    @PostMapping("/orders/{orderNumber}/reorder")
    @Operation(summary = "Put a past order back in a new cart",
            description = "Anything sold out is left out. Store the returned cartId (merge it after login).")
    public ResponseEntity<CartView> reorder(@PathVariable String orderNumber, @RequestParam String token) {
        return new ResponseEntity<>(linkService.reorder(orderNumber, token), HttpStatus.CREATED);
    }

    @PostMapping("/reviews/by-link")
    @Operation(summary = "Review a cookie from a delivered order", description = "Uses the order link token, so no account is needed.")
    public ResponseEntity<ReviewView> review(@RequestBody LinkedReviewRequest request) {
        return new ResponseEntity<>(linkService.review(request), HttpStatus.CREATED);
    }

    @PostMapping("/events/product-view")
    @Operation(summary = "A shopper viewed a cookie",
            description = "Only used when we know who they are (signed in, or their cart has an email). Always 202.")
    public ResponseEntity<Void> productView(@RequestBody ProductViewEvent event, Authentication authentication) {
        productViewService.viewed(event, CurrentUser.customerEmail(authentication));
        return ResponseEntity.accepted().build();
    }
}
