package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.OrderItem;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {

    List<OrderItem> findByOrderId(String orderId);

    List<OrderItem> findByOrderIdIn(Collection<String> orderIds);

    @Query("select count(i) > 0 from OrderItem i, CustomerOrder o where i.orderId = o.orderId "
            + "and o.customerEmail = :email and i.cookieId = :cookieId and o.status in :statuses")
    boolean hasBought(@Param("email") String email, @Param("cookieId") String cookieId,
                      @Param("statuses") Collection<OrderStatus> statuses);

    // Individual cookies, not packs.
    @Query("select coalesce(sum(i.quantity * i.unitsPerPack), 0) from OrderItem i, CustomerOrder o "
            + "where i.orderId = o.orderId and o.status in :statuses")
    long countCookies(@Param("statuses") Collection<OrderStatus> statuses);
}
