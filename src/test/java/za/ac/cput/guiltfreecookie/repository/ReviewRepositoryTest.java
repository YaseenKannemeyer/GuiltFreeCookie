package za.ac.cput.guiltfreecookie.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.Review;
import za.ac.cput.guiltfreecookie.domain.ReviewStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private CookieRepository cookieRepository;

    private Review save(String cookieId, String email, int rating, ReviewStatus status, String body) {
        return reviewRepository.saveAndFlush(new Review.Builder()
                .setCookieId(cookieId)
                .setCustomerEmail(email)
                .setRating(rating)
                .setTitle("Title")
                .setBody(body)
                .setAuthorName("Sam")
                .setStatus(status)
                .setCreatedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void statsAndTestimonialsOnlyCountApprovedReviewsOfCookiesOnSale() {
        Cookie cookie = cookieRepository.save(new Cookie.Builder().setDescription("Cookie").setSlug("cookie").build());
        save(cookie.getCookieId(), "a@example.com", 5, ReviewStatus.APPROVED, "A longer review");
        save(cookie.getCookieId(), "b@example.com", 3, ReviewStatus.APPROVED, "Short");
        save(cookie.getCookieId(), "c@example.com", 1, ReviewStatus.PENDING, "Pending");

        List<Object[]> ratings = reviewRepository.ratings(List.of(cookie.getCookieId()));
        assertEquals(4.0, ((Number) ratings.getFirst()[1]).doubleValue());
        assertEquals(2L, ratings.getFirst()[2]);
        assertEquals(2, reviewRepository.histogram(cookie.getCookieId()).size());
        assertEquals(2L, reviewRepository.overall().getFirst()[1]);
        assertEquals(1, reviewRepository.featured(PageRequest.of(0, 6)).size());

        cookieRepository.saveAndFlush(new Cookie.Builder().copy(cookie).setArchived(true).build());
        assertTrue(reviewRepository.featured(PageRequest.of(0, 6)).isEmpty());
    }

    @Test
    void theDatabaseRejectsASecondReviewFromTheSameCustomer() {
        save("c1", "a@example.com", 5, ReviewStatus.PENDING, "Body");

        assertThrows(DataIntegrityViolationException.class,
                () -> save("c1", "a@example.com", 4, ReviewStatus.PENDING, "Again"));
    }
}
