package za.ac.cput.guiltfreecookie.service.catalog;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.domain.CookieImage;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.domain.StockStatus;
import za.ac.cput.guiltfreecookie.dto.catalog.CatalogQuery;
import za.ac.cput.guiltfreecookie.dto.catalog.CategoryCount;
import za.ac.cput.guiltfreecookie.dto.catalog.CookieDetail;
import za.ac.cput.guiltfreecookie.dto.catalog.CookieSummary;
import za.ac.cput.guiltfreecookie.dto.catalog.ImageView;
import za.ac.cput.guiltfreecookie.dto.catalog.Nutrition;
import za.ac.cput.guiltfreecookie.dto.catalog.PageResponse;
import za.ac.cput.guiltfreecookie.dto.catalog.VariantView;
import za.ac.cput.guiltfreecookie.dto.engagement.RatingSummary;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CookieImageRepository;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CookieVariantRepository;
import za.ac.cput.guiltfreecookie.service.engagement.ReviewService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    static final int DEFAULT_PAGE_SIZE = 12;
    static final int MAX_PAGE_SIZE = 48;
    static final int MAX_QUERY_LENGTH = 100;
    static final int RELATED_COUNT = 4;

    private final CookieRepository cookieRepository;
    private final CookieVariantRepository variantRepository;
    private final CookieImageRepository imageRepository;
    private final StockRules stockRules;
    private final ReviewService reviews;

    public CatalogService(CookieRepository cookieRepository, CookieVariantRepository variantRepository,
                          CookieImageRepository imageRepository, StockRules stockRules, ReviewService reviews) {
        this.cookieRepository = cookieRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.stockRules = stockRules;
        this.reviews = reviews;
    }

    public PageResponse<CookieSummary> search(CatalogQuery query) {
        int page = query.page() == null ? 0 : query.page();
        int size = query.size() == null ? DEFAULT_PAGE_SIZE : query.size();
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE);
        }
        Page<Cookie> results = cookieRepository.findAll(specFor(query), PageRequest.of(page, size, sortFor(query.sort())));
        return PageResponse.of(results, this::summaries);
    }

    public CookieDetail detail(String idOrSlug) {
        Cookie cookie = cookieRepository.findById(idOrSlug)
                .or(() -> cookieRepository.findBySlug(idOrSlug))
                .filter(found -> !found.isArchived())
                .orElseThrow(() -> new NotFoundException("Cookie not found"));

        List<CookieVariant> active = variantRepository.findByCookieIdOrderBySortOrderAscUnitsPerPackAsc(cookie.getCookieId())
                .stream().filter(CookieVariant::isActive).toList();
        StockStatus status = stockRules.status(cookie, active);
        List<ImageView> images = imageRepository.findByCookieIdOrderBySortOrderAsc(cookie.getCookieId()).stream()
                .map(CatalogService::toImageView).toList();

        var rating = reviews.ratings(List.of(cookie.getCookieId())).getOrDefault(cookie.getCookieId(), new RatingSummary(null, 0));
        return new CookieDetail(
                cookie.getCookieId(),
                cookie.getSlug(),
                cookie.getDescription(),
                cookie.getCategory(),
                cookie.getLongDescription(),
                cookie.getIngredients(),
                cookie.getAllergies(),
                cookie.getImage(),
                images,
                active.stream().map(variant -> toVariantView(cookie, variant)).toList(),
                fromPrice(cookie, active),
                cookie.isFeatured(),
                status,
                status == StockStatus.LOW_STOCK ? cookie.getStockQuantity() : null,
                new Nutrition(cookie.getWeightGrams(), cookie.getCalories(), cookie.getSugarGrams(),
                        cookie.getProteinGrams(), cookie.getCarbsGrams(), cookie.getFatGrams()),
                rating.averageRating(),
                Math.toIntExact(rating.reviewCount()),
                related(cookie));
    }

    public List<CategoryCount> categories() {
        Map<CookieCategory, Long> counts = new EnumMap<>(CookieCategory.class);
        for (Object[] row : cookieRepository.countActiveByCategory()) {
            if (row[0] != null) {
                counts.put((CookieCategory) row[0], (Long) row[1]);
            }
        }
        List<CategoryCount> result = new ArrayList<>();
        for (CookieCategory category : CookieCategory.values()) {
            result.add(new CategoryCount(category, label(category), counts.getOrDefault(category, 0L)));
        }
        return result;
    }

    public List<CookieSummary> summaries(List<Cookie> cookies) {
        if (cookies.isEmpty()) {
            return List.of();
        }
        Map<String, List<CookieVariant>> variantsByCookie = variantRepository
                .findByCookieIdInAndActiveTrue(cookies.stream().map(Cookie::getCookieId).toList())
                .stream().collect(Collectors.groupingBy(CookieVariant::getCookieId));
        var ratings = reviews.ratings(cookies.stream().map(Cookie::getCookieId).toList());
        return cookies.stream().map(cookie -> {
            var rating = ratings.getOrDefault(cookie.getCookieId(), new RatingSummary(null, 0));
            List<CookieVariant> active = variantsByCookie.getOrDefault(cookie.getCookieId(), List.of());
            return new CookieSummary(
                    cookie.getCookieId(),
                    cookie.getSlug(),
                    cookie.getDescription(),
                    cookie.getCategory(),
                    cookie.getImage(),
                    fromPrice(cookie, active),
                    cookie.isFeatured(),
                    stockRules.status(cookie, active),
                    rating.averageRating(),
                    Math.toIntExact(rating.reviewCount()));
        }).toList();
    }

    public VariantView toVariantView(Cookie cookie, CookieVariant variant) {
        BigDecimal price = variant.effectivePrice(cookie);
        return new VariantView(
                variant.getVariantId(),
                variant.getLabel(),
                variant.getUnitsPerPack(),
                price,
                price.divide(BigDecimal.valueOf(variant.getUnitsPerPack()), 2, RoundingMode.HALF_UP),
                cookie.getStockQuantity() >= variant.getUnitsPerPack());
    }

    static String label(CookieCategory category) {
        String words = category.name().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private List<CookieSummary> related(Cookie cookie) {
        if (cookie.getCategory() == null) {
            return List.of();
        }
        List<CookieSummary> candidates = summaries(cookieRepository
                .findTop8ByCategoryAndArchivedFalseAndCookieIdNot(cookie.getCategory(), cookie.getCookieId()));
        return candidates.stream()
                .sorted(Comparator.comparing((CookieSummary summary) -> summary.stockStatus() == StockStatus.OUT_OF_STOCK))
                .limit(RELATED_COUNT)
                .toList();
    }

    private static BigDecimal fromPrice(Cookie cookie, List<CookieVariant> active) {
        return active.stream()
                .map(variant -> variant.effectivePrice(cookie))
                .min(Comparator.naturalOrder())
                .orElse(cookie.getPrice() == null ? null : BigDecimal.valueOf(cookie.getPrice()).setScale(2, RoundingMode.HALF_UP));
    }

    private static ImageView toImageView(CookieImage image) {
        return new ImageView(image.getImageId(), image.getImage(), image.getAltText(), image.getSortOrder());
    }

    private static Specification<Cookie> specFor(CatalogQuery query) {
        String q = query.q() == null ? null : query.q().trim().toLowerCase(Locale.ROOT);
        if (q != null && q.length() > MAX_QUERY_LENGTH) {
            throw new BadRequestException("Search text must be at most " + MAX_QUERY_LENGTH + " characters");
        }
        if (query.minPrice() != null && query.maxPrice() != null && query.minPrice() > query.maxPrice()) {
            throw new BadRequestException("minPrice can't be more than maxPrice");
        }
        return (root, criteria, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("archived")));
            if (q != null && !q.isEmpty()) {
                String pattern = "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), pattern, '\\'),
                        cb.like(cb.lower(root.get("longDescription")), pattern, '\\'),
                        cb.like(cb.lower(root.get("ingredients")), pattern, '\\')));
            }
            if (query.category() != null) {
                predicates.add(cb.equal(root.get("category"), query.category()));
            }
            if (query.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), query.minPrice()));
            }
            if (query.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), query.maxPrice()));
            }
            if (Boolean.TRUE.equals(query.inStock())) {
                predicates.add(cb.greaterThan(cb.coalesce(root.get("stockQuantity"), 0), 0));
            }
            if (Boolean.TRUE.equals(query.featured())) {
                predicates.add(cb.isTrue(root.get("featured")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    static Sort sortFor(String sort) {
        if (sort == null || sort.isBlank() || sort.equals("featured")) {
            return Sort.by(Sort.Order.desc("featured"), Sort.Order.asc("description"));
        }
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("description"));
            case "price_desc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("description"));
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("description"));
            case "name" -> Sort.by(Sort.Order.asc("description"));
            default -> throw new BadRequestException("sort must be one of featured, price_asc, price_desc, newest, name");
        };
    }
}
