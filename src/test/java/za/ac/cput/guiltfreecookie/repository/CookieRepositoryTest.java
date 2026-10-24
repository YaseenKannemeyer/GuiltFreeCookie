package za.ac.cput.guiltfreecookie.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.dto.catalog.CatalogQuery;
import za.ac.cput.guiltfreecookie.dto.catalog.CookieSummary;
import za.ac.cput.guiltfreecookie.dto.catalog.PageResponse;
import za.ac.cput.guiltfreecookie.service.catalog.CatalogService;
import za.ac.cput.guiltfreecookie.service.catalog.StockRules;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Runs the real queries (atomic stock update, search specification) against H2.
@DataJpaTest
@Import({CatalogService.class, StockRules.class, za.ac.cput.guiltfreecookie.service.engagement.ReviewService.class, za.ac.cput.guiltfreecookie.service.engagement.NoPurchaseVerifier.class})
class CookieRepositoryTest {

    @Autowired
    private CookieRepository cookieRepository;
    @Autowired
    private CatalogService catalogService;

    private String chunkId;

    @BeforeEach
    void setUp() {
        chunkId = save("Double Chocolate Chunk", CookieCategory.LOW_SUGAR, 32.0, 10, true, "Almond flour, cocoa").getCookieId();
        save("Lemon Almond", CookieCategory.GLUTEN_FREE, 30.0, 0, false, "Almond flour, lemon");
        save("Peanut Butter Crunch", CookieCategory.HIGH_PROTEIN, 28.0, 5, false, "Peanut butter, whey");
        save("100% Cocoa Bomb", CookieCategory.LOW_SUGAR, 40.0, 5, false, "Cocoa");
        Cookie archived = save("Old Favourite", CookieCategory.LOW_SUGAR, 20.0, 50, false, "Flour");
        cookieRepository.save(new Cookie.Builder().copy(archived).setArchived(true).build());
    }

    private Cookie save(String name, CookieCategory category, double price, int stock, boolean featured, String ingredients) {
        return cookieRepository.save(new Cookie.Builder()
                .setDescription(name).setCategory(category).setPrice(price).setStockQuantity(stock)
                .setFeatured(featured).setIngredients(ingredients).setAllergies("None listed")
                .setCreatedAt(LocalDateTime.now()).build());
    }

    private List<String> names(PageResponse<CookieSummary> page) {
        return page.items().stream().map(CookieSummary::name).toList();
    }

    private CatalogQuery query(String q, CookieCategory category, Double min, Double max, Boolean inStock, String sort) {
        return new CatalogQuery(q, category, min, max, inStock, null, sort, null, null);
    }

    @Test
    void adjustStockNeverGoesBelowZero() {
        assertEquals(1, cookieRepository.adjustStock(chunkId, -10));
        assertEquals(0, cookieRepository.adjustStock(chunkId, -1));
        assertEquals(0, cookieRepository.findById(chunkId).orElseThrow().getStockQuantity());
        assertEquals(1, cookieRepository.adjustStock(chunkId, 6));
        assertEquals(6, cookieRepository.findById(chunkId).orElseThrow().getStockQuantity());
    }

    @Test
    void searchHidesArchivedAndPutsFeaturedFirst() {
        PageResponse<CookieSummary> page = catalogService.search(query(null, null, null, null, null, null));

        assertEquals(4, page.totalItems());
        assertEquals("Double Chocolate Chunk", page.items().getFirst().name());
    }

    @Test
    void searchMatchesNameAndIngredientsCaseInsensitively() {
        assertEquals(List.of("Double Chocolate Chunk", "Lemon Almond"),
                names(catalogService.search(query("ALMOND", null, null, null, null, "name"))));
    }

    @Test
    void searchTreatsWildcardsLiterally() {
        assertEquals(List.of("100% Cocoa Bomb"), names(catalogService.search(query("100%", null, null, null, null, null))));
        assertEquals(List.of(), names(catalogService.search(query("_", null, null, null, null, null))));
    }

    @Test
    void searchFiltersByCategoryPriceAndStock() {
        assertEquals(List.of("100% Cocoa Bomb", "Double Chocolate Chunk"),
                names(catalogService.search(query(null, CookieCategory.LOW_SUGAR, null, null, null, "name"))));
        assertEquals(List.of("Peanut Butter Crunch", "Lemon Almond"),
                names(catalogService.search(query(null, null, null, 30.0, null, "price_asc"))));
        assertEquals(3, catalogService.search(query(null, null, null, null, true, null)).totalItems());
    }

    @Test
    void categoriesCountOnlyActiveCookies() {
        long lowSugar = catalogService.categories().stream()
                .filter(c -> c.category() == CookieCategory.LOW_SUGAR).findFirst().orElseThrow().count();

        assertEquals(2, lowSugar);
    }
}
