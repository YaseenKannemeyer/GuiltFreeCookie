package za.ac.cput.guiltfreecookie.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import za.ac.cput.guiltfreecookie.domain.*;

@DataJpaTest
class ReviewRepositoryTest {

    @Autowired
    ReviewRepository reviews;

    @Autowired
    CookieRepository cookies;

    Review save(
        String id,
        String email,
        int rating,
        ReviewStatus status,
        String body
    ) {
        return reviews.saveAndFlush(
            new Review.Builder()
                .setCookieId(id)
                .setCustomerEmail(email)
                .setRating(rating)
                .setTitle("Title")
                .setBody(body)
                .setAuthorName("Sam")
                .setStatus(status)
                .setCreatedAt(LocalDateTime.now())
                .build()
        );
    }

    @Test
    void aggregatesAndFeaturedExcludeUnapprovedReviews() {
        var c = cookies.save(
            new Cookie.Builder()
                .setDescription("Cookie")
                .setSlug("cookie")
                .build()
        );
        save(
            c.getCookieId(),
            "a@example.com",
            5,
            ReviewStatus.APPROVED,
            "A longer review"
        );
        save(
            c.getCookieId(),
            "b@example.com",
            3,
            ReviewStatus.APPROVED,
            "Short"
        );
        save(
            c.getCookieId(),
            "c@example.com",
            1,
            ReviewStatus.PENDING,
            "Pending"
        );
        var rows = reviews.ratings(List.of(c.getCookieId()));
        assertEquals(4.0, ((Number) rows.getFirst()[1]).doubleValue());
        assertEquals(2L, rows.getFirst()[2]);
        assertEquals(2, reviews.histogram(c.getCookieId()).size());
        assertEquals(2L, reviews.overall().getFirst()[1]);
        assertEquals(1, reviews.featured(PageRequest.of(0, 6)).size());
        cookies.saveAndFlush(
            new Cookie.Builder().copy(c).setArchived(true).build()
        );
        assertTrue(reviews.featured(PageRequest.of(0, 6)).isEmpty());
    }

    @Test
    void uniqueConstraintPreventsConcurrentDuplicates() {
        save("c1", "a@example.com", 5, ReviewStatus.PENDING, "Body");
        assertThrows(DataIntegrityViolationException.class, () ->
            save("c1", "a@example.com", 4, ReviewStatus.PENDING, "Again")
        );
    }
}
