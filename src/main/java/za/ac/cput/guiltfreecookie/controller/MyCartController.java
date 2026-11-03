package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.dto.cart.CartView;
import za.ac.cput.guiltfreecookie.dto.cart.MergeCartRequest;
import za.ac.cput.guiltfreecookie.service.cart.ShopCartService;

@RestController
@RequestMapping("/api/v1/me/cart")
@Tag(name = "My account")
public class MyCartController {

    private final ShopCartService cartService;

    public MyCartController(ShopCartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "My saved cart", description = "404 if I don't have one yet.")
    public ResponseEntity<CartView> myCart(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(cartService.myCart(email));
    }

    @PostMapping("/merge")
    @Operation(summary = "Bring the guest cart into my account", description = "Call right after login with the cartId kept in the browser. Use the returned cartId from now on.")
    public ResponseEntity<CartView> merge(@AuthenticationPrincipal String email, @RequestBody MergeCartRequest request) {
        return ResponseEntity.ok(cartService.merge(email, request.cartId()));
    }
}
