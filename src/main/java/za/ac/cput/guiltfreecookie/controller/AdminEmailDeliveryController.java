package za.ac.cput.guiltfreecookie.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.service.notification.EmailOutbox;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/email-deliveries")
public class AdminEmailDeliveryController {
    private final EmailOutbox outbox;
    public AdminEmailDeliveryController(EmailOutbox outbox) { this.outbox = outbox; }
    @GetMapping
    public ResponseEntity<List<EmailOutbox.DeliveryView>> recent() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(outbox.recent());
    }
    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retry(@PathVariable String id) {
        outbox.retry(id);
        return ResponseEntity.accepted().build();
    }
}
