package za.ac.cput.guiltfreecookie.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.Cart;
import za.ac.cput.guiltfreecookie.domain.CartStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, String> {

    List<Cart> findByCustomerEmail(String customerEmail);

    List<Cart> findByStatus(CartStatus status);

    // Serialises changes to one cart (two quick "add" clicks can't both insert the same line).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.cartId = :cartId")
    Optional<Cart> findForUpdate(@Param("cartId") String cartId);

    Optional<Cart> findFirstByCustomerEmailAndAccountOwnedTrueAndStatusOrderByUpdatedAtDesc(
            String customerEmail, CartStatus status);
}