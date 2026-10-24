package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.CookieImage;

import java.util.List;

@Repository
public interface CookieImageRepository extends JpaRepository<CookieImage, String> {

    List<CookieImage> findByCookieIdOrderBySortOrderAsc(String cookieId);

    long countByCookieId(String cookieId);
}
