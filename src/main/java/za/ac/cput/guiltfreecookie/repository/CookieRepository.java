package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;

import java.util.List;
import java.util.Optional;

@Repository
public interface CookieRepository extends JpaRepository<Cookie, String>, JpaSpecificationExecutor<Cookie> {

    List<Cookie> findByArchivedFalse();

    Optional<Cookie> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Cookie> findTop8ByCategoryAndArchivedFalseAndCookieIdNot(CookieCategory category, String cookieId);

    @Query("select c.category, count(c) from Cookie c where c.archived = false group by c.category")
    List<Object[]> countActiveByCategory();

    // Atomic: never takes stock below zero, even with concurrent orders. Returns rows changed (0 = not enough stock).
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Cookie c set c.stockQuantity = coalesce(c.stockQuantity, 0) + :change "
            + "where c.cookieId = :cookieId and coalesce(c.stockQuantity, 0) + :change >= 0")
    int adjustStock(@Param("cookieId") String cookieId, @Param("change") int change);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Cookie c set c.stockQuantity = :quantity where c.cookieId = :cookieId")
    int setStock(@Param("cookieId") String cookieId, @Param("quantity") int quantity);
}
