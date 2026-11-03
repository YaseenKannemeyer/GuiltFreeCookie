package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.DiscountCode;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountCodeRepository extends JpaRepository<DiscountCode, String> {

    Optional<DiscountCode> findByCode(String code);

    boolean existsByCode(String code);

    List<DiscountCode> findAllByOrderByCreatedAtDesc();

    // Atomic: two orders can't both take the last use. Returns 0 when the code is used up.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DiscountCode d set d.usedCount = d.usedCount + 1 "
            + "where d.code = :code and (d.maxUses is null or d.usedCount < d.maxUses)")
    int redeem(@Param("code") String code);

    // Gives a use back when an unpaid order is cancelled.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DiscountCode d set d.usedCount = d.usedCount - 1 where d.code = :code and d.usedCount > 0")
    int release(@Param("code") String code);
}
