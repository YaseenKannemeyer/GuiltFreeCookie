package za.ac.cput.guiltfreecookie.service.catalog;

import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.domain.StockStatus;
import za.ac.cput.guiltfreecookie.dto.catalog.AdminCookieView;
import za.ac.cput.guiltfreecookie.dto.catalog.CookieDetailsRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CookieImageRepository;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CookieVariantRepository;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.util.Comparator;
import java.util.List;

@Service
public class CatalogAdminService {

    private final CookieRepository cookieRepository;
    private final CookieVariantRepository variantRepository;
    private final CookieImageRepository imageRepository;
    private final StockRules stockRules;

    public CatalogAdminService(CookieRepository cookieRepository, CookieVariantRepository variantRepository,
                               CookieImageRepository imageRepository, StockRules stockRules) {
        this.cookieRepository = cookieRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.stockRules = stockRules;
    }

    public List<AdminCookieView> list() {
        return cookieRepository.findAll().stream()
                .sorted(Comparator.comparing(Cookie::getDescription, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::view)
                .toList();
    }

    public AdminCookieView get(String cookieId) {
        return view(require(cookieId));
    }

    // Active cookies that are low or out, most urgent first.
    public List<AdminCookieView> lowStock() {
        return cookieRepository.findByArchivedFalse().stream()
                .map(this::view)
                .filter(view -> view.stockStatus() != StockStatus.IN_STOCK)
                .sorted(Comparator.comparing((AdminCookieView view) -> view.stockStatus() != StockStatus.OUT_OF_STOCK)
                        .thenComparing(view -> view.cookie().getStockQuantity()))
                .toList();
    }

    // Replaces all of these fields; slug is kept when not sent.
    public AdminCookieView updateDetails(String cookieId, CookieDetailsRequest request) {
        Cookie existing = require(cookieId);
        String slug = request.slug() == null ? existing.getSlug() : Helper.clean(request.slug());
        if (!Helper.isValidSlug(slug)) {
            throw new BadRequestException("Slug must be lowercase letters, numbers and dashes (max " + Helper.MAX_SLUG_LENGTH + ")");
        }
        if (!slug.equals(existing.getSlug()) && cookieRepository.existsBySlug(slug)) {
            throw new ConflictException("Another cookie already uses that slug");
        }
        Cookie updated = new Cookie.Builder()
                .copy(existing)
                .setSlug(slug)
                .setFeatured(Boolean.TRUE.equals(request.featured()))
                .setLowStockThreshold(inRange(request.lowStockThreshold(), 0, 10_000, "lowStockThreshold"))
                .setWeightGrams(inRange(request.weightGrams(), 1, 1_000, "weightGrams"))
                .setCalories(inRange(request.calories(), 0, 5_000, "calories"))
                .setSugarGrams(grams(request.sugarGrams(), "sugarGrams"))
                .setProteinGrams(grams(request.proteinGrams(), "proteinGrams"))
                .setCarbsGrams(grams(request.carbsGrams(), "carbsGrams"))
                .setFatGrams(grams(request.fatGrams(), "fatGrams"))
                .build();
        return view(cookieRepository.save(updated));
    }

    AdminCookieView view(Cookie cookie) {
        List<CookieVariant> variants = variantRepository.findByCookieIdOrderBySortOrderAscUnitsPerPackAsc(cookie.getCookieId());
        return new AdminCookieView(
                cookie,
                stockRules.status(cookie, variants.stream().filter(CookieVariant::isActive).toList()),
                variants,
                imageRepository.findByCookieIdOrderBySortOrderAsc(cookie.getCookieId()));
    }

    private Cookie require(String cookieId) {
        return cookieRepository.findById(cookieId).orElseThrow(() -> new NotFoundException("Cookie not found"));
    }

    private static Integer inRange(Integer value, int min, int max, String field) {
        if (value != null && (value < min || value > max)) {
            throw new BadRequestException(field + " must be between " + min + " and " + max);
        }
        return value;
    }

    private static Double grams(Double value, String field) {
        if (value != null && (value < 0 || value > 1_000 || value.isNaN())) {
            throw new BadRequestException(field + " must be between 0 and 1000");
        }
        return value;
    }
}
