package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.domain.ShippingSettings;
import za.ac.cput.guiltfreecookie.dto.shipping.ShippingSettingsRequest;
import za.ac.cput.guiltfreecookie.service.shipping.ShippingSettingsService;

@RestController
@RequestMapping("/api/v1/admin/shipping-settings")
@Tag(name = "Admin shipping", description = "Delivery rates, free-delivery threshold and the pickup point. Admin token required.")
public class AdminShippingController {

    private final ShippingSettingsService settingsService;

    public AdminShippingController(ShippingSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<ShippingSettings> get() {
        return ResponseEntity.ok(settingsService.get());
    }

    @PutMapping
    @Operation(summary = "Update delivery settings", description = "Leave freeShippingThreshold empty to switch free delivery off.")
    public ResponseEntity<ShippingSettings> update(@RequestBody ShippingSettingsRequest request) {
        return ResponseEntity.ok(settingsService.update(request));
    }
}
