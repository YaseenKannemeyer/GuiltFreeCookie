package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.dto.cart.AddItemRequest;
import za.ac.cput.guiltfreecookie.dto.cart.CartEmailRequest;
import za.ac.cput.guiltfreecookie.dto.cart.CartView;
import za.ac.cput.guiltfreecookie.dto.cart.DiscountCodeRequest;
import za.ac.cput.guiltfreecookie.dto.cart.QuantityRequest;
import za.ac.cput.guiltfreecookie.security.CurrentUser;
import za.ac.cput.guiltfreecookie.service.cart.ShopCartService;

// The storefront cart. /cart/* (admin) is the older admin view of the same carts.
@RestController
@RequestMapping("/api/v1/carts")
@Tag(name = "Cart", description = "Guest-first cart. Keep the cartId in the browser; every call returns the full priced cart.")
public class ShopCartController {

    private final ShopCartService cartService;

    public ShopCartController(ShopCartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    @Operation(summary = "Start a cart", description = "With a customer token, returns their existing cart if they have one.")
    public ResponseEntity<CartView> create(Authentication authentication) {
        return new ResponseEntity<>(cartService.create(CurrentUser.customerEmail(authentication)), HttpStatus.CREATED);
    }

    @GetMapping("/{cartId}")
    public ResponseEntity<CartView> get(@PathVariable String cartId) {
        return ResponseEntity.ok(cartService.get(cartId));
    }

    @PostMapping("/{cartId}/items")
    @Operation(summary = "Add a pack size", description = "Adding the same pack again increases its quantity.")
    public ResponseEntity<CartView> addItem(@PathVariable String cartId, @RequestBody AddItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(cartId, request));
    }

    @PutMapping("/{cartId}/items/{cartItemId}")
    @Operation(summary = "Change quantity", description = "Quantity 0 removes the item.")
    public ResponseEntity<CartView> updateQuantity(
            @PathVariable String cartId, @PathVariable String cartItemId, @RequestBody QuantityRequest request) {
        return ResponseEntity.ok(cartService.updateQuantity(cartId, cartItemId, request.quantity()));
    }

    @DeleteMapping("/{cartId}/items/{cartItemId}")
    public ResponseEntity<CartView> removeItem(@PathVariable String cartId, @PathVariable String cartItemId) {
        return ResponseEntity.ok(cartService.removeItem(cartId, cartItemId));
    }

    @DeleteMapping("/{cartId}/items")
    @Operation(summary = "Empty the cart")
    public ResponseEntity<CartView> clear(@PathVariable String cartId) {
        return ResponseEntity.ok(cartService.clear(cartId));
    }

    @PostMapping("/{cartId}/discount")
    @Operation(summary = "Apply a discount code", description = "400 with a customer-friendly message if the code can't be used.")
    public ResponseEntity<CartView> applyDiscount(@PathVariable String cartId, @RequestBody DiscountCodeRequest request) {
        return ResponseEntity.ok(cartService.applyDiscount(cartId, request.code()));
    }

    @DeleteMapping("/{cartId}/discount")
    public ResponseEntity<CartView> removeDiscount(@PathVariable String cartId) {
        return ResponseEntity.ok(cartService.removeDiscount(cartId));
    }

    @PutMapping("/{cartId}/email")
    @Operation(summary = "Save the guest's email on the cart", description = "Ask for it early in checkout: it enables abandoned-cart reminders.")
    public ResponseEntity<CartView> setEmail(@PathVariable String cartId, @RequestBody CartEmailRequest request) {
        return ResponseEntity.ok(cartService.setEmail(cartId, request.email()));
    }
}
