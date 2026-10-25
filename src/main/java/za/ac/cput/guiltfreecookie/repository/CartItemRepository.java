package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.CartItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, String> {

    List<CartItem> findByCartIdOrderByAddedAtAsc(String cartId);

    Optional<CartItem> findByCartIdAndVariantId(String cartId, String variantId);

    long countByCartId(String cartId);

    @Query("select count(i) > 0 from CartItem i, Cart c where i.cartId = c.cartId"
            + " and c.customerEmail = :email and i.addedAt > :since")
    boolean addedByEmailSince(@Param("email") String email, @Param("since") LocalDateTime since);

    void deleteByCartId(String cartId);
}
