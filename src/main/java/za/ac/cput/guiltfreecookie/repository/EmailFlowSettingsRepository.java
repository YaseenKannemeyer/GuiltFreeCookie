package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.EmailFlowSettings;

@Repository
public interface EmailFlowSettingsRepository extends JpaRepository<EmailFlowSettings, String> {
}
