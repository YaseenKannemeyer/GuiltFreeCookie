package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.EmailSend;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailSendRepository extends JpaRepository<EmailSend, String> {

    boolean existsByEmailAndSentAtAfter(String email, LocalDateTime time);

    // [flow, sent, opened, clicked] for each flow sent in [from, to).
    @Query("select s.flow, count(s), count(s.openedAt), count(s.clickedAt) from EmailSend s"
            + " where s.sentAt >= :from and s.sentAt < :to group by s.flow")
    List<Object[]> statsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<EmailSend> findByClickedAtGreaterThanEqualAndClickedAtLessThan(LocalDateTime from, LocalDateTime to);

    // [flow, sent, opened, clicked] for each flow since the given time.
    @Query("select s.flow, count(s), count(s.openedAt), count(s.clickedAt) from EmailSend s"
            + " where s.sentAt >= :since group by s.flow")
    List<Object[]> statsSince(@Param("since") LocalDateTime since);
}
