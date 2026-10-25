package za.ac.cput.guiltfreecookie.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, String>, JpaSpecificationExecutor<CustomerOrder> {

    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    // Serialises status changes on one order (e.g. a payment notification racing the expiry job).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.orderNumber = :orderNumber")
    Optional<CustomerOrder> findForUpdate(@Param("orderNumber") String orderNumber);

    List<CustomerOrder> findByStatusAndReservedUntilBefore(OrderStatus status, LocalDateTime time);

    boolean existsByCustomerEmailAndDiscountCodeAndStatusIn(String customerEmail, String discountCode,
                                                            Collection<OrderStatus> statuses);

    boolean existsByCustomerEmailAndStatusIn(String customerEmail, Collection<OrderStatus> statuses);

    long countByCustomerEmailAndStatus(String customerEmail, OrderStatus status);

    List<CustomerOrder> findByStatusInAndPaidAtGreaterThanEqualAndPaidAtLessThan(Collection<OrderStatus> statuses,
                                                                                LocalDateTime from, LocalDateTime to);

    long countByStatusAndPaidAtGreaterThanEqualAndPaidAtLessThan(OrderStatus status, LocalDateTime from, LocalDateTime to);

    // A cart that's checked out, cancelled and checked out again counts once.
    @Query("select count(distinct coalesce(o.cartId, o.orderId)) from CustomerOrder o where o.createdAt >= :from and o.createdAt < :to")
    long countCheckedOutCarts(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndStatusIn(LocalDateTime from, LocalDateTime to,
                                                                          Collection<OrderStatus> statuses);

    // Which of these emails had a paid order before the given time.
    @Query("select distinct o.customerEmail from CustomerOrder o where o.customerEmail in :emails"
            + " and o.status in :statuses and o.paidAt < :before")
    List<String> emailsPaidBefore(@Param("emails") Collection<String> emails,
                                  @Param("statuses") Collection<OrderStatus> statuses, @Param("before") LocalDateTime before);

    boolean existsByCustomerEmailAndStatusInAndPaidAtAfter(String customerEmail, Collection<OrderStatus> statuses,
                                                           LocalDateTime time);

    // [email, last paid at] for customers whose most recent paid order is between the two times.
    @Query("select o.customerEmail, max(o.paidAt) from CustomerOrder o where o.status in :statuses"
            + " group by o.customerEmail having max(o.paidAt) < :before and max(o.paidAt) > :after")
    List<Object[]> lastPaidBetween(@Param("statuses") Collection<OrderStatus> statuses,
                                   @Param("after") LocalDateTime after, @Param("before") LocalDateTime before);

    @Query("select count(distinct o.customerEmail) from CustomerOrder o where o.status in :statuses")
    long countCustomers(@Param("statuses") Collection<OrderStatus> statuses);
}
