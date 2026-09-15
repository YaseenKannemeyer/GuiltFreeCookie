package za.ac.cput.guiltfreecookie.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.domain.DeliveryDetails;
import za.ac.cput.guiltfreecookie.service.DeliveryDetailsService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/delivery-details")
public class DeliveryDetailsController {

    private final DeliveryDetailsService service;

    public DeliveryDetailsController(DeliveryDetailsService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public ResponseEntity<DeliveryDetails> createDeliveryDetails(
            @RequestBody DeliveryDetails deliveryDetails) {

        DeliveryDetails created = service.create(deliveryDetails);

        return ResponseEntity.ok(created);
    }

    @GetMapping("/all")
    public ResponseEntity<List<DeliveryDetails>> getAll() {

        List<DeliveryDetails> deliveryDetails = service.getAll();

        return ResponseEntity.ok(deliveryDetails);
    }

    @GetMapping("/read/{id}")
    public ResponseEntity<DeliveryDetails> readDeliveryDetails(
            @PathVariable String id) {

        DeliveryDetails deliveryDetails = service.read(id);

        return ResponseEntity.ok(deliveryDetails);
    }

    @PutMapping("/update")
    public ResponseEntity<DeliveryDetails> updateDeliveryDetails(
            @RequestBody DeliveryDetails deliveryDetails) {

        DeliveryDetails updated = service.update(deliveryDetails);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Boolean> deleteDeliveryDetails(
            @PathVariable String id) {

        boolean deleted = service.delete(id);

        return ResponseEntity.ok(deleted);
    }
}
