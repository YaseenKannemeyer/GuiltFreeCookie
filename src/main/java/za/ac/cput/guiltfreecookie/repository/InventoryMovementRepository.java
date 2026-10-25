package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.InventoryMovement;
import za.ac.cput.guiltfreecookie.domain.InventoryReason;

import java.util.Collection;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, String> {

    List<InventoryMovement> findByCookieIdOrderByCreatedAtDesc(String cookieId, Pageable pageable);

    // Net stock change logged against one order: negative while it's holding cookies.
    @Query("select coalesce(sum(m.quantityChange), 0) from InventoryMovement m where m.note = :note and m.reason in :reasons")
    long netChange(@Param("note") String note, @Param("reasons") Collection<InventoryReason> reasons);
}
