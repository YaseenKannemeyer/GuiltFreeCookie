package za.ac.cput.guiltfreecookie.service.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.domain.StockStatus;
import za.ac.cput.guiltfreecookie.dto.catalog.CatalogQuery;
import za.ac.cput.guiltfreecookie.dto.catalog.CookieDetail;
import za.ac.cput.guiltfreecookie.dto.catalog.CookieSummary;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CookieImageRepository;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CookieVariantRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private CookieRepository cookieRepository;
    @Mock
    private CookieVariantRepository variantRepository;
    @Mock
    private CookieImageRepository imageRepository;
    private CatalogService service;

    private final Cookie chunk = new Cookie.Builder()
            .setCookieId("CK1").setSlug("double-chocolate-chunk").setDescription("Double Chocolate Chunk")
            .setCategory(CookieCategory.LOW_SUGAR).setPrice(32.00).setStockQuantity(8).setCalories(160).build();
    private final CookieVariant single = new CookieVariant.Builder()
            .setVariantId("V1").setCookieId("CK1").setLabel("Single").setUnitsPerPack(1).build();
    private final CookieVariant box = new CookieVariant.Builder()
            .setVariantId("V6").setCookieId("CK1").setLabel("Box of 6").setUnitsPerPack(6).setPrice(new BigDecimal("175")).build();
    private final CookieVariant hidden = new CookieVariant.Builder()
            .setVariantId("V12").setCookieId("CK1").setLabel("Box of 12").setUnitsPerPack(12).setActive(false).build();

    @BeforeEach
    void setUp() {
        service = new CatalogService(cookieRepository, variantRepository, imageRepository, new StockRules(12), org.mockito.Mockito.mock(za.ac.cput.guiltfreecookie.service.engagement.ReviewService.class));
    }

    @Test
    void detailBySlugShowsActivePacksWithAvailabilityAndPricePerCookie() {
        when(cookieRepository.findById("double-chocolate-chunk")).thenReturn(Optional.empty());
        when(cookieRepository.findBySlug("double-chocolate-chunk")).thenReturn(Optional.of(chunk));
        when(variantRepository.findByCookieIdOrderBySortOrderAscUnitsPerPackAsc("CK1")).thenReturn(List.of(single, box, hidden));
        when(imageRepository.findByCookieIdOrderBySortOrderAsc("CK1")).thenReturn(List.of());
        when(cookieRepository.findTop8ByCategoryAndArchivedFalseAndCookieIdNot(CookieCategory.LOW_SUGAR, "CK1")).thenReturn(List.of());

        CookieDetail detail = service.detail("double-chocolate-chunk");

        assertEquals("Double Chocolate Chunk", detail.name());
        assertEquals(2, detail.variants().size());
        assertTrue(detail.variants().get(0).available());
        assertTrue(detail.variants().get(1).available());
        assertEquals(new BigDecimal("29.17"), detail.variants().get(1).pricePerCookie());
        assertEquals(new BigDecimal("32.00"), detail.fromPrice());
        assertEquals(StockStatus.LOW_STOCK, detail.stockStatus());
        assertEquals(8, detail.lowStockRemaining());
        assertEquals(160, detail.nutrition().calories());
    }

    @Test
    void archivedCookiesAreNotFound() {
        Cookie archived = new Cookie.Builder().copy(chunk).setArchived(true).build();
        when(cookieRepository.findById("CK1")).thenReturn(Optional.of(archived));

        assertThrows(NotFoundException.class, () -> service.detail("CK1"));
    }

    @Test
    void summariesUseTheCheapestActivePackAsFromPrice() {
        Cookie plenty = new Cookie.Builder().copy(chunk).setStockQuantity(100).build();
        CookieVariant cheapBox = new CookieVariant.Builder()
                .setCookieId("CK1").setUnitsPerPack(1).setPrice(new BigDecimal("30")).build();
        when(variantRepository.findByCookieIdInAndActiveTrue(anyList())).thenReturn(List.of(single, cheapBox));

        CookieSummary summary = service.summaries(List.of(plenty)).getFirst();

        assertEquals(new BigDecimal("30.00"), summary.fromPrice());
        assertEquals(StockStatus.IN_STOCK, summary.stockStatus());
        assertNull(summary.averageRating());
    }

    @Test
    void aBoxThatCantBeFilledIsUnavailable() {
        Cookie low = new Cookie.Builder().copy(chunk).setStockQuantity(4).build();

        assertFalse(service.toVariantView(low, box).available());
        assertTrue(service.toVariantView(low, single).available());
    }

    @Test
    void sortOptionsAreValidated() {
        assertEquals(Sort.Order.desc("featured"), CatalogService.sortFor(null).iterator().next());
        assertEquals(Sort.Order.asc("price"), CatalogService.sortFor("price_asc").iterator().next());
        assertThrows(BadRequestException.class, () -> CatalogService.sortFor("cheapest"));
    }

    @Test
    void searchValidatesPagingAndRanges() {
        assertThrows(BadRequestException.class,
                () -> service.search(new CatalogQuery(null, null, null, null, null, null, null, 0, 500)));
        assertThrows(BadRequestException.class,
                () -> service.search(new CatalogQuery(null, null, 50.0, 10.0, null, null, null, null, null)));
        assertThrows(BadRequestException.class,
                () -> service.search(new CatalogQuery("x".repeat(101), null, null, null, null, null, null, null, null)));
    }

    @Test
    void categoryLabelsMatchTheFrontend() {
        assertEquals("Low sugar", CatalogService.label(CookieCategory.LOW_SUGAR));
        assertEquals("Diabetic friendly", CatalogService.label(CookieCategory.DIABETIC_FRIENDLY));
    }
}
