package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.Review;
import za.ac.cput.guiltfreecookie.domain.ReviewStatus;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {

    String APPROVED = "za.ac.cput.guiltfreecookie.domain.ReviewStatus.APPROVED";

    boolean existsByCookieIdAndCustomerEmail(String cookieId, String customerEmail);

    Page<Review> findByCookieIdAndStatus(String cookieId, ReviewStatus status, Pageable pageable);

    List<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status);

    List<Review> findAllByOrderByCreatedAtDesc();

    // Rows of [cookieId, average rating, count] for approved reviews.
    @Query("select r.cookieId, avg(r.rating), count(r) from Review r "
            + "where r.status = " + APPROVED + " and r.cookieId in :cookieIds group by r.cookieId")
    List<Object[]> ratings(@Param("cookieIds") Collection<String> cookieIds);

    // Rows of [rating, count] for one cookie's approved reviews.
    @Query("select r.rating, count(r) from Review r "
            + "where r.status = " + APPROVED + " and r.cookieId = :cookieId group by r.rating")
    List<Object[]> histogram(@Param("cookieId") String cookieId);

    // One row of [average rating, count] across the whole shop.
    @Query("select avg(r.rating), count(r) from Review r where r.status = " + APPROVED)
    List<Object[]> overall();

    // Homepage testimonials: 4 and 5 star reviews of cookies still on sale, longest first.
    @Query("select r from Review r where r.status = " + APPROVED + " and r.rating >= 4 "
            + "and exists (select c.cookieId from Cookie c where c.cookieId = r.cookieId and c.archived = false) "
            + "order by length(r.body) desc, r.createdAt desc, r.id")
    List<Review> featured(Pageable pageable);
}
