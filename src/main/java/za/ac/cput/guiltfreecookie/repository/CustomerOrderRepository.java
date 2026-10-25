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
