package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.ShippingSettings;

@Repository
public interface ShippingSettingsRepository extends JpaRepository<ShippingSettings, String> {
}
