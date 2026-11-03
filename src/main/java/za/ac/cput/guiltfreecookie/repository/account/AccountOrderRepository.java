package za.ac.cput.guiltfreecookie.repository.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountOrderRepository extends Repository<CustomerOrder, String> {

    // Leaves out abandoned PayFast attempts: unpaid orders past their hold, and cancelled or failed ones never paid.
    @Query("select o from CustomerOrder o where o.customerEmail = :email and o.accountOwned = true"
            + " and (o.status <> za.ac.cput.guiltfreecookie.domain.OrderStatus.PENDING_PAYMENT or o.reservedUntil > :now)"
            + " and (o.status not in (za.ac.cput.guiltfreecookie.domain.OrderStatus.CANCELLED,"
            + " za.ac.cput.guiltfreecookie.domain.OrderStatus.PAYMENT_FAILED) or o.paidAt is not null)")
    Page<CustomerOrder> history(@Param("email") String email, @Param("now") LocalDateTime now, Pageable pageable);

    Optional<CustomerOrder> findByOrderNumberAndCustomerEmailAndAccountOwnedTrue(String orderNumber, String email);

    @Query("select count(o) from CustomerOrder o where o.customerEmail = :email and o.accountOwned = false and o.paidAt is not null")
    long countClaimable(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.customerEmail = :email and o.accountOwned = false and o.paidAt is not null"
            + " order by o.orderId")
    List<CustomerOrder> lockClaimable(@Param("email") String email);

    CustomerOrder save(CustomerOrder order);
}
