package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.EmailFlow;
import za.ac.cput.guiltfreecookie.domain.EnrollmentStatus;
import za.ac.cput.guiltfreecookie.domain.FlowEnrollment;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface FlowEnrollmentRepository extends JpaRepository<FlowEnrollment, String> {

    List<FlowEnrollment> findByStatusAndNextSendAtLessThanEqualOrderByNextSendAtAsc(
            EnrollmentStatus status, LocalDateTime time, Pageable pageable);

    List<FlowEnrollment> findByEmailAndStatus(String email, EnrollmentStatus status);

    boolean existsByEmailAndFlowAndStatus(String email, EmailFlow flow, EnrollmentStatus status);

    boolean existsByEmailAndFlow(String email, EmailFlow flow);

    boolean existsByFlowAndContextRef(EmailFlow flow, String contextRef);

    boolean existsByEmailAndFlowAndEnrolledAtAfter(String email, EmailFlow flow, LocalDateTime time);

    List<FlowEnrollment> findByEmailAndFlowInAndStatus(String email, Collection<EmailFlow> flows, EnrollmentStatus status);
}
