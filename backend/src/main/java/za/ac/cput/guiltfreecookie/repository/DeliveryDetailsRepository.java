package za.ac.cput.guiltfreecookie.repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.DeliveryDetails;

@Repository
public interface DeliveryDetailsRepository extends JpaRepository<DeliveryDetails, String> {

    Boolean existsByDeliveryID(String deliveryID);
}
