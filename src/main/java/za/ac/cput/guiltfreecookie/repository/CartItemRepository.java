package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.CartItem;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, String> {

    List<CartItem> findByCartIdOrderByAddedAtAsc(String cartId);

    Optional<CartItem> findByCartIdAndVariantId(String cartId, String variantId);

    long countByCartId(String cartId);

    void deleteByCartId(String cartId);
}
