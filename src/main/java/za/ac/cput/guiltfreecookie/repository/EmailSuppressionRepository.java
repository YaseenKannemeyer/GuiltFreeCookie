package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.EmailSuppression;

@Repository
public interface EmailSuppressionRepository extends JpaRepository<EmailSuppression, String> {
}
