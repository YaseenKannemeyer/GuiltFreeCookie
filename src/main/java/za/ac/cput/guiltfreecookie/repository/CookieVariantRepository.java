package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;

import java.util.Collection;
import java.util.List;

@Repository
public interface CookieVariantRepository extends JpaRepository<CookieVariant, String> {

    List<CookieVariant> findByCookieIdOrderBySortOrderAscUnitsPerPackAsc(String cookieId);

    List<CookieVariant> findByCookieIdInAndActiveTrue(Collection<String> cookieIds);

    boolean existsByCookieId(String cookieId);

    boolean existsByCookieIdAndLabelIgnoreCase(String cookieId, String label);

    void deleteByCookieId(String cookieId);
}
