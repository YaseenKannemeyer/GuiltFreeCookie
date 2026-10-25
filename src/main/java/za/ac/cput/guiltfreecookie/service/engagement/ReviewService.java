package za.ac.cput.guiltfreecookie.service.engagement;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.Review;
import za.ac.cput.guiltfreecookie.domain.ReviewStatus;
import za.ac.cput.guiltfreecookie.dto.catalog.PageResponse;
import za.ac.cput.guiltfreecookie.dto.engagement.RatingSummary;
import za.ac.cput.guiltfreecookie.dto.engagement.ReviewPage;
import za.ac.cput.guiltfreecookie.dto.engagement.ReviewRequest;
import za.ac.cput.guiltfreecookie.dto.engagement.ReviewSummary;
import za.ac.cput.guiltfreecookie.dto.engagement.ReviewView;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.ReviewRepository;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    static final int MAX_TITLE_LENGTH = 100;
    static final int MAX_BODY_LENGTH = 2000;
    static final int MAX_AUTHOR_LENGTH = 100;
    static final int MAX_PAGE_SIZE = 48;
    static final int MAX_FEATURED = 12;
    static final String ALREADY_REVIEWED = "You have already reviewed this cookie";

    private final ReviewRepository reviewRepository;
    private final CookieRepository cookieRepository;
    private final PurchaseVerifier purchaseVerifier;

    public ReviewService(ReviewRepository reviewRepository, CookieRepository cookieRepository,
                         PurchaseVerifier purchaseVerifier) {
        this.reviewRepository = reviewRepository;
        this.cookieRepository = cookieRepository;
        this.purchaseVerifier = purchaseVerifier;
    }

    // One review per customer per cookie, including ones that were rejected. Starts as PENDING.
    @Transactional
    public ReviewView create(String email, String idOrSlug, ReviewRequest request) {
        return create(email, idOrSlug, request, null);
    }

    // From an order's emailed review link: the link proves the purchase, so it's a verified buyer.
    @Transactional
    public ReviewView createFromOrderLink(String email, String idOrSlug, ReviewRequest request) {
        return create(email, idOrSlug, request, true);
    }

    private ReviewView create(String email, String idOrSlug, ReviewRequest request, Boolean verified) {
        Cookie cookie = requireCookie(idOrSlug);
        String title = Helper.clean(request.title());
        String body = Helper.clean(request.body());
        String authorName = Helper.clean(request.authorName());
        if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw new BadRequestException("Choose a rating from 1 to 5");
        }
        if (!Helper.isPresent(title, MAX_TITLE_LENGTH)
                || !Helper.isPresent(body, MAX_BODY_LENGTH)
                || !Helper.isPresent(authorName, MAX_AUTHOR_LENGTH)) {
            throw new BadRequestException("Enter your name (" + MAX_AUTHOR_LENGTH + " characters), title ("
                    + MAX_TITLE_LENGTH + ") and review (" + MAX_BODY_LENGTH + " or fewer)");
        }
        if (reviewRepository.existsByCookieIdAndCustomerEmail(cookie.getCookieId(), email)) {
            throw new ConflictException(ALREADY_REVIEWED);
        }
        try {
            return view(reviewRepository.saveAndFlush(new Review.Builder()
                    .setCookieId(cookie.getCookieId())
                    .setCustomerEmail(email)
                    .setRating(request.rating())
                    .setTitle(title)
                    .setBody(body)
                    .setAuthorName(authorName)
                    .setVerifiedBuyer(verified != null ? verified : purchaseVerifier.hasBought(email, cookie.getCookieId()))
                    .setStatus(ReviewStatus.PENDING)
                    .setCreatedAt(LocalDateTime.now())
                    .build()));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(ALREADY_REVIEWED);
        }
    }

    public ReviewPage list(String idOrSlug, int page, int size, String sort) {
        String cookieId = requireCookie(idOrSlug).getCookieId();
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("page must be non-negative and size between 1 and " + MAX_PAGE_SIZE);
        }
        Sort ordering = switch (sort) {
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"));
            case "highest" -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt"));
            case "lowest" -> Sort.by(Sort.Order.asc("rating"), Sort.Order.desc("createdAt"));
            default -> throw new BadRequestException("sort must be newest, highest or lowest");
        };

        Map<Integer, Long> histogram = new LinkedHashMap<>();
        for (int stars = 5; stars >= 1; stars--) {
            histogram.put(stars, 0L);
        }
        for (Object[] row : reviewRepository.histogram(cookieId)) {
            histogram.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        long count = 0;
        long total = 0;
        for (Map.Entry<Integer, Long> entry : histogram.entrySet()) {
            count += entry.getValue();
            total += entry.getKey() * entry.getValue();
        }

        PageRequest pageRequest = PageRequest.of(page, size, ordering.and(Sort.by("id")));
        return new ReviewPage(
                new ReviewSummary(count == 0 ? null : (double) total / count, count, histogram),
                PageResponse.of(reviewRepository.findByCookieIdAndStatus(cookieId, ReviewStatus.APPROVED, pageRequest),
                        reviews -> reviews.stream().map(ReviewService::view).toList()));
    }

    // Approved-review averages for a batch of cookies, in one query.
    public Map<String, RatingSummary> ratings(Collection<String> cookieIds) {
        Map<String, RatingSummary> result = new HashMap<>();
        if (cookieIds.isEmpty()) {
            return result;
        }
        for (Object[] row : reviewRepository.ratings(cookieIds)) {
            result.put((String) row[0], new RatingSummary(((Number) row[1]).doubleValue(), ((Number) row[2]).longValue()));
        }
        return result;
    }

    public RatingSummary overall() {
        List<Object[]> rows = reviewRepository.overall();
        if (rows.isEmpty() || rows.getFirst()[0] == null) {
            return new RatingSummary(null, 0);
        }
        Object[] row = rows.getFirst();
        return new RatingSummary(((Number) row[0]).doubleValue(), ((Number) row[1]).longValue());
    }

    public List<ReviewView> featured(int limit) {
        if (limit < 1 || limit > MAX_FEATURED) {
            throw new BadRequestException("limit must be between 1 and " + MAX_FEATURED);
        }
        return reviewRepository.featured(PageRequest.of(0, limit)).stream().map(ReviewService::view).toList();
    }

    public List<ReviewView> moderationList(ReviewStatus status) {
        List<Review> reviews = status == null
                ? reviewRepository.findAllByOrderByCreatedAtDesc()
                : reviewRepository.findByStatusOrderByCreatedAtDesc(status);
        return reviews.stream().map(ReviewService::view).toList();
    }

    @Transactional
    public ReviewView moderate(String reviewId, ReviewStatus status) {
        Review existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        return view(reviewRepository.save(new Review.Builder()
                .copy(existing)
                .setStatus(status)
                .setModeratedAt(LocalDateTime.now())
                .build()));
    }

    public static ReviewView view(Review review) {
        return new ReviewView(
                review.getId(),
                review.getCookieId(),
                review.getRating(),
                review.getTitle(),
                review.getBody(),
                review.getAuthorName(),
                review.isVerifiedBuyer(),
                review.getStatus().name(),
                review.getCreatedAt(),
                review.getModeratedAt());
    }

    private Cookie requireCookie(String idOrSlug) {
        return cookieRepository.findById(idOrSlug)
                .or(() -> cookieRepository.findBySlug(idOrSlug))
                .filter(cookie -> !cookie.isArchived())
                .orElseThrow(() -> new NotFoundException("Cookie not found"));
    }
}
