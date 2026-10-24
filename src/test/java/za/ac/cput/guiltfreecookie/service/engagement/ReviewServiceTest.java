package za.ac.cput.guiltfreecookie.service.engagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import za.ac.cput.guiltfreecookie.domain.*;
import za.ac.cput.guiltfreecookie.dto.engagement.*;
import za.ac.cput.guiltfreecookie.exception.*;
import za.ac.cput.guiltfreecookie.repository.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    ReviewRepository reviews;

    @Mock
    CookieRepository cookies;

    @Mock
    PurchaseVerifier verifier;

    ReviewService service;
    Cookie cookie = new Cookie.Builder()
        .setCookieId("c1")
        .setSlug("chocolate")
        .setDescription("Chocolate")
        .build();
    ReviewRequest request = new ReviewRequest(
        5,
        " Great ",
        " Delicious ",
        " Sam "
    );

    @BeforeEach
    void setup() {
        service = new ReviewService(reviews, cookies, verifier);
    }

    @Test
    void newReviewIsCleanedPendingAndVerifiedByServer() {
        when(cookies.findById("c1")).thenReturn(Optional.of(cookie));
        when(reviews.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(verifier.hasBought("sam@example.com", "c1")).thenReturn(true);
        var view = service.create("sam@example.com", "c1", request);
        assertEquals("PENDING", view.status());
        assertEquals("Great", view.title());
        assertTrue(view.verifiedBuyer());
        assertTrue(
            Arrays.stream(ReviewView.class.getRecordComponents()).noneMatch(c ->
                c.getName().toLowerCase().contains("email")
            )
        );
    }

    @Test
    void duplicateIsConflictEvenWhenFirstReviewWasRejected() {
        when(cookies.findById("c1")).thenReturn(Optional.of(cookie));
        when(
            reviews.existsByCookieIdAndCustomerEmail("c1", "sam@example.com")
        ).thenReturn(true);
        assertThrows(ConflictException.class, () ->
            service.create("sam@example.com", "c1", request)
        );
        verify(reviews, never()).saveAndFlush(any());
    }

    @Test
    void ratingsAndLengthsAreValidated() {
        when(cookies.findById("c1")).thenReturn(Optional.of(cookie));
        for (Integer rating : Arrays.asList(null, 0, 6))
            assertThrows(BadRequestException.class, () ->
                service.create(
                    "s",
                    "c1",
                    new ReviewRequest(rating, "Title", "Body", "Name")
                )
            );
        assertThrows(BadRequestException.class, () ->
            service.create(
                "s",
                "c1",
                new ReviewRequest(5, "x".repeat(101), "Body", "Name")
            )
        );
    }

    @Test
    void approvedPageHasCompleteHistogramAndStableSort() {
        when(cookies.findById("chocolate")).thenReturn(Optional.empty());
        when(cookies.findBySlug("chocolate")).thenReturn(Optional.of(cookie));
        when(reviews.histogram("c1")).thenReturn(
            List.<Object[]>of(new Object[] { 5, 2L }, new Object[] { 3, 1L })
        );
        when(
            reviews.findByCookieIdAndStatus(
                eq("c1"),
                eq(ReviewStatus.APPROVED),
                any()
            )
        ).thenReturn(Page.empty());
        var result = service.list("chocolate", 0, 12, "highest");
        assertEquals(3, result.summary().reviewCount());
        assertEquals(13.0 / 3, result.summary().averageRating());
        assertEquals(0, result.summary().histogram().get(1));
        assertThrows(BadRequestException.class, () ->
            service.list("chocolate", -1, 12, "newest")
        );
        assertThrows(BadRequestException.class, () ->
            service.list("chocolate", 0, 12, "invalid")
        );
    }

    @Test
    void ratingLookupIsBatchedAndEmptyLookupDoesNotQuery() {
        when(reviews.ratings(List.of("a", "b"))).thenReturn(
            List.<Object[]>of(new Object[] { "a", 4.5, 2L })
        );
        assertEquals(
            4.5,
            service.ratings(List.of("a", "b")).get("a").averageRating()
        );
        assertTrue(service.ratings(List.of()).isEmpty());
        verify(reviews, times(1)).ratings(any());
    }

    @Test
    void moderationRecordsTime() {
        var review = new Review.Builder()
            .setId("r1")
            .setStatus(ReviewStatus.PENDING)
            .setCreatedAt(LocalDateTime.now())
            .build();
        when(reviews.findById("r1")).thenReturn(Optional.of(review));
        when(reviews.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.moderate("r1", ReviewStatus.REJECTED);
        assertEquals("REJECTED", result.status());
        assertNotNull(result.moderatedAt());
        assertThrows(NotFoundException.class, () ->
            service.moderate("missing", ReviewStatus.APPROVED)
        );
    }

    @Test
    void archivedCookieCannotReceiveReviews() {
        when(cookies.findById("c1")).thenReturn(
            Optional.of(
                new Cookie.Builder().copy(cookie).setArchived(true).build()
            )
        );
        assertThrows(NotFoundException.class, () ->
            service.create("s", "c1", request)
        );
    }

    @Test
    void defaultVerifierNeverInventsPurchases() {
        assertFalse(new NoPurchaseVerifier().hasBought("s", "c1"));
    }
}
