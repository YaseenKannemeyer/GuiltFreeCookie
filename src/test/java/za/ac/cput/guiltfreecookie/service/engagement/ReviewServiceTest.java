package za.ac.cput.guiltfreecookie.service.engagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.Review;
import za.ac.cput.guiltfreecookie.domain.ReviewStatus;
import za.ac.cput.guiltfreecookie.dto.engagement.ReviewPage;
import za.ac.cput.guiltfreecookie.dto.engagement.ReviewRequest;
import za.ac.cput.guiltfreecookie.dto.engagement.ReviewView;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.ReviewRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private CookieRepository cookieRepository;
    @Mock
    private PurchaseVerifier purchaseVerifier;
    @InjectMocks
    private ReviewService service;

    private final Cookie cookie = new Cookie.Builder().setCookieId("c1").setSlug("chocolate").setDescription("Chocolate").build();
    private final ReviewRequest request = new ReviewRequest(5, " Great ", " Delicious ", " Sam ");

    @Test
    void aNewReviewIsCleanedPendingAndVerifiedByTheServer() {
        when(cookieRepository.findById("c1")).thenReturn(Optional.of(cookie));
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(call -> call.getArgument(0));
        when(purchaseVerifier.hasBought("sam@example.com", "c1")).thenReturn(true);

        ReviewView view = service.create("sam@example.com", "c1", request);

        assertEquals("PENDING", view.status());
        assertEquals("Great", view.title());
        assertTrue(view.verifiedBuyer());
        assertTrue(Arrays.stream(ReviewView.class.getRecordComponents())
                .noneMatch(component -> component.getName().toLowerCase().contains("email")));
    }

    @Test
    void aSecondReviewIsAConflictEvenIfTheFirstWasRejected() {
        when(cookieRepository.findById("c1")).thenReturn(Optional.of(cookie));
        when(reviewRepository.existsByCookieIdAndCustomerEmail("c1", "sam@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create("sam@example.com", "c1", request));
        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void ratingsAndLengthsAreValidated() {
        when(cookieRepository.findById("c1")).thenReturn(Optional.of(cookie));

        for (Integer rating : Arrays.asList(null, 0, 6)) {
            assertThrows(BadRequestException.class,
                    () -> service.create("s", "c1", new ReviewRequest(rating, "Title", "Body", "Name")));
        }
        assertThrows(BadRequestException.class,
                () -> service.create("s", "c1", new ReviewRequest(5, "x".repeat(101), "Body", "Name")));
    }

    @Test
    void thePageHasAFullHistogramAndValidatesPaging() {
        when(cookieRepository.findById("chocolate")).thenReturn(Optional.empty());
        when(cookieRepository.findBySlug("chocolate")).thenReturn(Optional.of(cookie));
        when(reviewRepository.histogram("c1")).thenReturn(List.of(new Object[]{5, 2L}, new Object[]{3, 1L}));
        when(reviewRepository.findByCookieIdAndStatus(eq("c1"), eq(ReviewStatus.APPROVED), any())).thenReturn(Page.empty());

        ReviewPage result = service.list("chocolate", 0, 12, "highest");

        assertEquals(3, result.summary().reviewCount());
        assertEquals(13.0 / 3, result.summary().averageRating());
        assertEquals(0, result.summary().histogram().get(1));
        assertThrows(BadRequestException.class, () -> service.list("chocolate", -1, 12, "newest"));
        assertThrows(BadRequestException.class, () -> service.list("chocolate", 0, 12, "invalid"));
    }

    @Test
    void ratingsAreLookedUpInOneBatchAndAnEmptyBatchSkipsTheQuery() {
        when(reviewRepository.ratings(List.of("a", "b"))).thenReturn(List.<Object[]>of(new Object[]{"a", 4.5, 2L}));

        assertEquals(4.5, service.ratings(List.of("a", "b")).get("a").averageRating());
        assertTrue(service.ratings(List.of()).isEmpty());
        verify(reviewRepository, times(1)).ratings(any());
    }

    @Test
    void moderationRecordsWhen() {
        Review review = new Review.Builder().setId("r1").setStatus(ReviewStatus.PENDING).setCreatedAt(LocalDateTime.now()).build();
        when(reviewRepository.findById("r1")).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(call -> call.getArgument(0));

        ReviewView result = service.moderate("r1", ReviewStatus.REJECTED);

        assertEquals("REJECTED", result.status());
        assertNotNull(result.moderatedAt());
        assertThrows(NotFoundException.class, () -> service.moderate("missing", ReviewStatus.APPROVED));
    }

    @Test
    void anArchivedCookieCantBeReviewed() {
        when(cookieRepository.findById("c1")).thenReturn(Optional.of(new Cookie.Builder().copy(cookie).setArchived(true).build()));

        assertThrows(NotFoundException.class, () -> service.create("s", "c1", request));
    }
}
