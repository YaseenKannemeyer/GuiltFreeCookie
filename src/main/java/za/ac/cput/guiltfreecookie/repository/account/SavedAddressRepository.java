package za.ac.cput.guiltfreecookie.repository.account;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.cput.guiltfreecookie.domain.DeliveryDetails;

import java.util.List;
import java.util.Optional;

// Saved addresses are delivery_details rows with a customerEmail.
public interface SavedAddressRepository extends JpaRepository<DeliveryDetails, String> {

    List<DeliveryDetails> findByCustomerEmailOrderByCreatedAtDescDeliveryIDDesc(String email);

    Optional<DeliveryDetails> findByDeliveryIDAndCustomerEmail(String id, String email);
}
