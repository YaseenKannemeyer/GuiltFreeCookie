package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.dto.catalog.CatalogQuery;
import za.ac.cput.guiltfreecookie.dto.catalog.CategoryCount;
import za.ac.cput.guiltfreecookie.dto.catalog.CookieDetail;
import za.ac.cput.guiltfreecookie.dto.catalog.CookieSummary;
import za.ac.cput.guiltfreecookie.dto.catalog.PageResponse;
import za.ac.cput.guiltfreecookie.service.catalog.CatalogService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cookies")
@Tag(name = "Catalog", description = "Public product listing, search and product detail.")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @Operation(summary = "Search and filter the catalog")
    public ResponseEntity<PageResponse<CookieSummary>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CookieCategory category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Boolean featured,
            @Parameter(description = "featured (default), price_asc, price_desc, newest, name")
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(catalogService.search(
                new CatalogQuery(q, category, minPrice, maxPrice, inStock, featured, sort, page, size)));
    }

    @GetMapping("/categories")
    @Operation(summary = "Every category with its customer-facing label and number of cookies")
    public ResponseEntity<List<CategoryCount>> categories() {
        return ResponseEntity.ok(catalogService.categories());
    }

    @GetMapping("/{idOrSlug}")
    @Operation(summary = "Product detail by id or slug")
    public ResponseEntity<CookieDetail> detail(@PathVariable String idOrSlug) {
        return ResponseEntity.ok(catalogService.detail(idOrSlug));
    }
}
