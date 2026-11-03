package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.domain.DiscountCode;
import za.ac.cput.guiltfreecookie.dto.discount.DiscountRequest;
import za.ac.cput.guiltfreecookie.service.discount.DiscountService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/discounts")
@Tag(name = "Admin discounts", description = "Discount codes. Admin token required.")
public class AdminDiscountController {

    private final DiscountService discountService;

    public AdminDiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @GetMapping
    public ResponseEntity<List<DiscountCode>> list() {
        return ResponseEntity.ok(discountService.list());
    }

    @GetMapping("/{discountId}")
    public ResponseEntity<DiscountCode> get(@PathVariable String discountId) {
        return ResponseEntity.ok(discountService.get(discountId));
    }

    @PostMapping
    @Operation(summary = "Create a code", description = "PERCENT (value 1–100), FIXED (rand) or FREE_SHIPPING (no value).")
    public ResponseEntity<DiscountCode> create(@RequestBody DiscountRequest request) {
        return new ResponseEntity<>(discountService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{discountId}")
    public ResponseEntity<DiscountCode> update(@PathVariable String discountId, @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(discountService.update(discountId, request));
    }

    @DeleteMapping("/{discountId}")
    @Operation(summary = "Switch a code off", description = "Deactivates it; it stays on record for past orders.")
    public ResponseEntity<DiscountCode> deactivate(@PathVariable String discountId) {
        return ResponseEntity.ok(discountService.deactivate(discountId));
    }
}
