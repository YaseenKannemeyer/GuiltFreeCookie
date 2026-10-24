package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.InventoryMovement;

import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, String> {

    List<InventoryMovement> findByCookieIdOrderByCreatedAtDesc(String cookieId, Pageable pageable);
}
