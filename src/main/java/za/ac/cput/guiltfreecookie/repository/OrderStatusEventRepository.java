package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.OrderStatusEvent;

import java.util.List;

@Repository
public interface OrderStatusEventRepository extends JpaRepository<OrderStatusEvent, String> {

    List<OrderStatusEvent> findByOrderIdOrderByCreatedAtAsc(String orderId);
}
