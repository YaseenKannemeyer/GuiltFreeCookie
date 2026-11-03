package za.ac.cput.guiltfreecookie.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.domain.DeliveryDetails;
import za.ac.cput.guiltfreecookie.repository.DeliveryDetailsRepository;

import java.util.List;

@Service
public class DeliveryDetailsService implements IService<DeliveryDetails, String> {

    private DeliveryDetailsRepository repository;

    @Autowired
    public DeliveryDetailsService(DeliveryDetailsRepository repository) {
        this.repository = repository;
    }

    @Override
    public DeliveryDetails create(DeliveryDetails deliveryDetails) {
        return repository.save(deliveryDetails);
    }

    @Override
    public DeliveryDetails read(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public DeliveryDetails update(DeliveryDetails deliveryDetails) {
        DeliveryDetails existing = deliveryDetails.getDeliveryID() == null ? null
                : repository.findById(deliveryDetails.getDeliveryID()).orElse(null);
        if (existing == null) {
            return null;
        }
        // Only the address changes here, so a customer's saved-address fields (owner, label, default) survive.
        return repository.save(new DeliveryDetails.Builder().copy(existing).setAddress(deliveryDetails.getAddress()).build());
    }

    @Override
    public boolean delete(String id) {
        this.repository.deleteById(id);
        return true;
    }

    @Override
    public List<DeliveryDetails> getAll() {
        return repository.findAll();
    }
}
