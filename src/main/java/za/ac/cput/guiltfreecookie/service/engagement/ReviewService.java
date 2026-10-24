package za.ac.cput.guiltfreecookie.service.engagement;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.*;
import za.ac.cput.guiltfreecookie.dto.catalog.PageResponse;
import za.ac.cput.guiltfreecookie.dto.engagement.*;
import za.ac.cput.guiltfreecookie.exception.*;
import za.ac.cput.guiltfreecookie.repository.*;
import za.ac.cput.guiltfreecookie.util.Helper;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviews;
    private final CookieRepository cookies;
    private final PurchaseVerifier purchases;

    public ReviewService(
        ReviewRepository reviews,
        CookieRepository cookies,
        PurchaseVerifier purchases
    ) {
        this.reviews = reviews;
        this.cookies = cookies;
        this.purchases = purchases;
    }

    private Cookie cookie(String key) {
        return cookies
            .findById(key)
            .or(() -> cookies.findBySlug(key))
            .filter(c -> !c.isArchived())
            .orElseThrow(() -> new NotFoundException("Cookie not found"));
    }

    @Transactional
    public ReviewView create(
        String email,
        String cookieId,
        ReviewRequest request
    ) {
        Cookie cookie = cookie(cookieId);
        String title = Helper.clean(request.title()),
            body = Helper.clean(request.body()),
            name = Helper.clean(request.authorName());
        if (
            request.rating() == null ||
            request.rating() < 1 ||
            request.rating() > 5
        ) throw new BadRequestException("Choose a rating from 1 to 5");
        if (
            !Helper.isPresent(title, 100) ||
            !Helper.isPresent(body, 2000) ||
            !Helper.isPresent(name, 100)
        ) throw new BadRequestException(
            "Enter your name (100 characters), title (100) and review (2000 or fewer)"
        );
        if (
            reviews.existsByCookieIdAndCustomerEmail(
                cookie.getCookieId(),
                email
            )
        ) throw new ConflictException("You have already reviewed this cookie");
        try {
            return view(
                reviews.saveAndFlush(
                    new Review.Builder()
                        .setCookieId(cookie.getCookieId())
                        .setCustomerEmail(email)
                        .setRating(request.rating())
                        .setTitle(title)
                        .setBody(body)
                        .setAuthorName(name)
                        .setVerifiedBuyer(
                            purchases.hasBought(email, cookie.getCookieId())
                        )
                        .setStatus(ReviewStatus.PENDING)
                        .setCreatedAt(LocalDateTime.now())
                        .build()
                )
            );
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(
                "You have already reviewed this cookie"
            );
        }
    }

    public ReviewPage list(String key, int page, int size, String sort) {
        String id = cookie(key).getCookieId();
        if (page < 0 || size < 1 || size > 48) throw new BadRequestException(
            "page must be non-negative and size between 1 and 48"
        );
        Sort ordering = switch (sort) {
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"));
            case "highest" -> Sort.by(
                Sort.Order.desc("rating"),
                Sort.Order.desc("createdAt")
            );
            case "lowest" -> Sort.by(
                Sort.Order.asc("rating"),
                Sort.Order.desc("createdAt")
            );
            default -> throw new BadRequestException(
                "sort must be newest, highest or lowest"
            );
        };
        Map<Integer, Long> histogram = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) histogram.put(i, 0L);
        for (Object[] row : reviews.histogram(id))
            histogram.put(
                ((Number) row[0]).intValue(),
                ((Number) row[1]).longValue()
            );
        long count = histogram
            .values()
            .stream()
            .mapToLong(Long::longValue)
            .sum();
        double sum = histogram
            .entrySet()
            .stream()
            .mapToDouble(e -> e.getKey() * e.getValue())
            .sum();
        return new ReviewPage(
            new ReviewSummary(
                count == 0 ? null : sum / count,
                count,
                histogram
            ),
            PageResponse.of(
                reviews.findByCookieIdAndStatus(
                    id,
                    ReviewStatus.APPROVED,
                    PageRequest.of(page, size, ordering.and(Sort.by("id")))
                ),
                items -> items.stream().map(ReviewService::view).toList()
            )
        );
    }

    public Map<String, RatingSummary> ratings(Collection<String> ids) {
        Map<String, RatingSummary> result = new HashMap<>();
        if (!ids.isEmpty()) for (Object[] row : reviews.ratings(ids))
            result.put(
                (String) row[0],
                new RatingSummary(
                    ((Number) row[1]).doubleValue(),
                    ((Number) row[2]).longValue()
                )
            );
        return result;
    }

    public RatingSummary overall() {
        List<Object[]> rows = reviews.overall();
        if (
            rows.isEmpty() || rows.getFirst()[0] == null
        ) return new RatingSummary(null, 0);
        return new RatingSummary(
            ((Number) rows.getFirst()[0]).doubleValue(),
            ((Number) rows.getFirst()[1]).longValue()
        );
    }

    public List<ReviewView> featured(int limit) {
        if (limit < 1 || limit > 12) throw new BadRequestException(
            "limit must be between 1 and 12"
        );
        return reviews
            .featured(PageRequest.of(0, limit))
            .stream()
            .map(ReviewService::view)
            .toList();
    }

    public List<ReviewView> moderateList(ReviewStatus status) {
        return (
            status == null
                ? reviews.findAllByOrderByCreatedAtDesc()
                : reviews.findByStatusOrderByCreatedAtDesc(status)
        ).stream()
            .map(ReviewService::view)
            .toList();
    }

    @Transactional
    public ReviewView moderate(String id, ReviewStatus status) {
        Review existing = reviews
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Review not found"));
        return view(
            reviews.save(
                new Review.Builder()
                    .copy(existing)
                    .setStatus(status)
                    .setModeratedAt(LocalDateTime.now())
                    .build()
            )
        );
    }

    public static ReviewView view(Review r) {
        return new ReviewView(
            r.getId(),
            r.getCookieId(),
            r.getRating(),
            r.getTitle(),
            r.getBody(),
            r.getAuthorName(),
            r.isVerifiedBuyer(),
            r.getStatus().name(),
            r.getCreatedAt(),
            r.getModeratedAt()
        );
    }
}
