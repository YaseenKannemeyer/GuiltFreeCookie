package za.ac.cput.guiltfreecookie.repository;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import za.ac.cput.guiltfreecookie.domain.*;

public interface ReviewRepository extends JpaRepository<Review, String> {
    boolean existsByCookieIdAndCustomerEmail(
        String cookieId,
        String customerEmail
    );
    Page<Review> findByCookieIdAndStatus(
        String cookieId,
        ReviewStatus status,
        Pageable pageable
    );
    List<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status);
    List<Review> findAllByOrderByCreatedAtDesc();

    @Query(
        "select r.cookieId, avg(r.rating), count(r) from Review r where r.status = za.ac.cput.guiltfreecookie.domain.ReviewStatus.APPROVED and r.cookieId in :ids group by r.cookieId"
    )
    List<Object[]> ratings(Collection<String> ids);

    @Query(
        "select r.rating, count(r) from Review r where r.status = za.ac.cput.guiltfreecookie.domain.ReviewStatus.APPROVED and r.cookieId = :id group by r.rating"
    )
    List<Object[]> histogram(String id);

    @Query(
        "select avg(r.rating), count(r) from Review r where r.status = za.ac.cput.guiltfreecookie.domain.ReviewStatus.APPROVED"
    )
    List<Object[]> overall();

    @Query(
        "select r from Review r where r.status = za.ac.cput.guiltfreecookie.domain.ReviewStatus.APPROVED and r.rating >= 4 and exists (select c.cookieId from Cookie c where c.cookieId = r.cookieId and c.archived = false) order by length(r.body) desc, r.createdAt desc, r.id"
    )
    List<Review> featured(Pageable pageable);
}
