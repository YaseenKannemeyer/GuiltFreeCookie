package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.ac.cput.guiltfreecookie.domain.CookieImage;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.domain.InventoryMovement;
import za.ac.cput.guiltfreecookie.dto.catalog.AdminCookieView;
import za.ac.cput.guiltfreecookie.dto.catalog.CookieDetailsRequest;
import za.ac.cput.guiltfreecookie.dto.catalog.ImageOrderRequest;
import za.ac.cput.guiltfreecookie.dto.catalog.StockAdjustRequest;
import za.ac.cput.guiltfreecookie.dto.catalog.VariantRequest;
import za.ac.cput.guiltfreecookie.service.catalog.CatalogAdminService;
import za.ac.cput.guiltfreecookie.service.catalog.CookieImageService;
import za.ac.cput.guiltfreecookie.service.catalog.CookieVariantService;
import za.ac.cput.guiltfreecookie.service.catalog.InventoryService;

import java.util.List;

// Create, edit, archive and delete stay on /cookie/*; this adds everything Phase 2 introduced.
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin catalog", description = "Stock, pack sizes, gallery photos and product details. Admin token required.")
public class AdminCatalogController {

    private final CatalogAdminService catalogAdminService;
    private final CookieVariantService variantService;
    private final CookieImageService imageService;
    private final InventoryService inventoryService;

    public AdminCatalogController(CatalogAdminService catalogAdminService, CookieVariantService variantService,
                                  CookieImageService imageService, InventoryService inventoryService) {
        this.catalogAdminService = catalogAdminService;
        this.variantService = variantService;
        this.imageService = imageService;
        this.inventoryService = inventoryService;
    }

    @GetMapping("/cookies")
    @Operation(summary = "Every cookie (archived too) with stock status, packs and photos")
    public ResponseEntity<List<AdminCookieView>> list() {
        return ResponseEntity.ok(catalogAdminService.list());
    }

    @GetMapping("/cookies/{cookieId}")
    public ResponseEntity<AdminCookieView> get(@PathVariable String cookieId) {
        return ResponseEntity.ok(catalogAdminService.get(cookieId));
    }

    @PutMapping("/cookies/{cookieId}/details")
    @Operation(summary = "Slug, featured flag, low-stock threshold and nutrition")
    public ResponseEntity<AdminCookieView> updateDetails(
            @PathVariable String cookieId, @RequestBody CookieDetailsRequest request) {
        return ResponseEntity.ok(catalogAdminService.updateDetails(cookieId, request));
    }

    @GetMapping("/cookies/{cookieId}/variants")
    public ResponseEntity<List<CookieVariant>> variants(@PathVariable String cookieId) {
        return ResponseEntity.ok(variantService.list(cookieId));
    }

    @PostMapping("/cookies/{cookieId}/variants")
    @Operation(summary = "Add a pack size", description = "Leave price empty to charge unit price x cookies per pack.")
    public ResponseEntity<CookieVariant> createVariant(
            @PathVariable String cookieId, @RequestBody VariantRequest request) {
        return new ResponseEntity<>(variantService.create(cookieId, request), HttpStatus.CREATED);
    }

    @PutMapping("/cookies/{cookieId}/variants/{variantId}")
    public ResponseEntity<CookieVariant> updateVariant(
            @PathVariable String cookieId, @PathVariable String variantId, @RequestBody VariantRequest request) {
        return ResponseEntity.ok(variantService.update(cookieId, variantId, request));
    }

    @DeleteMapping("/cookies/{cookieId}/variants/{variantId}")
    @Operation(summary = "Hide a pack size", description = "Deactivates it; carts and past orders may still refer to it.")
    public ResponseEntity<CookieVariant> deactivateVariant(@PathVariable String cookieId, @PathVariable String variantId) {
        return ResponseEntity.ok(variantService.deactivate(cookieId, variantId));
    }

    @PostMapping(value = "/cookies/{cookieId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add a gallery photo (JPEG, PNG, WebP or GIF)")
    public ResponseEntity<CookieImage> addImage(
            @PathVariable String cookieId,
            @RequestPart("image") MultipartFile image,
            @RequestPart(value = "altText", required = false) String altText) {
        return new ResponseEntity<>(imageService.add(cookieId, image, altText), HttpStatus.CREATED);
    }

    @PutMapping("/cookies/{cookieId}/images/order")
    @Operation(summary = "Reorder gallery photos", description = "Send every photo id for the cookie, in display order.")
    public ResponseEntity<List<CookieImage>> reorderImages(
            @PathVariable String cookieId, @RequestBody ImageOrderRequest request) {
        return ResponseEntity.ok(imageService.reorder(cookieId, request.imageIds()));
    }

    @DeleteMapping("/cookies/{cookieId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable String cookieId, @PathVariable String imageId) {
        imageService.delete(cookieId, imageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/cookies/{cookieId}/stock")
    @Operation(summary = "Change stock", description = "Send adjustBy (e.g. 24 or -3) or setTo (stocktake). Every change is logged.")
    public ResponseEntity<AdminCookieView> adjustStock(
            @PathVariable String cookieId,
            @RequestBody StockAdjustRequest request,
            @AuthenticationPrincipal String adminId) {
        inventoryService.adjust(cookieId, request, adminId);
        return ResponseEntity.ok(catalogAdminService.get(cookieId));
    }

    @GetMapping("/cookies/{cookieId}/inventory")
    @Operation(summary = "Stock change history, newest first")
    public ResponseEntity<List<InventoryMovement>> inventory(
            @PathVariable String cookieId, @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(inventoryService.history(cookieId, limit));
    }

    @GetMapping("/inventory/low-stock")
    @Operation(summary = "Active cookies that are low or out of stock, most urgent first")
    public ResponseEntity<List<AdminCookieView>> lowStock() {
        return ResponseEntity.ok(catalogAdminService.lowStock());
    }
}
