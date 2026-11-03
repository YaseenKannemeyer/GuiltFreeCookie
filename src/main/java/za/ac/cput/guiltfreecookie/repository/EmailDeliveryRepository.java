package za.ac.cput.guiltfreecookie.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import za.ac.cput.guiltfreecookie.domain.EmailDelivery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, String> {
    long countByStatus(EmailDelivery.Status status);
    boolean existsByStatusAndNextAttemptAtBefore(EmailDelivery.Status status, LocalDateTime before);
    List<EmailDelivery> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            EmailDelivery.Status status, LocalDateTime now, Pageable page);
    List<EmailDelivery> findAllByOrderByCreatedAtDesc(Pageable page);
    List<EmailDelivery> findByStatusInAndExpiresAtBefore(List<EmailDelivery.Status> statuses, LocalDateTime now, Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from EmailDelivery d where d.id = :id")
    Optional<EmailDelivery> lockById(@Param("id") String id);
    long deleteByCreatedAtBeforeAndPayloadIsNull(LocalDateTime before);
}
