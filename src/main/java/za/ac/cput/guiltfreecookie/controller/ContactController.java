package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.domain.ContactMessage;
import za.ac.cput.guiltfreecookie.domain.ContactStatus;
import za.ac.cput.guiltfreecookie.dto.engagement.ContactRequest;
import za.ac.cput.guiltfreecookie.service.engagement.ContactService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Contact", description = "Public contact form and the admin inbox.")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping("/contact")
    @Operation(summary = "Send a contact message", description = "Always 204. Rate-limited to 5 per hour per IP.")
    public ResponseEntity<Void> submit(@RequestBody ContactRequest request) {
        contactService.submit(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/messages")
    @Operation(summary = "Contact messages, newest first")
    public ResponseEntity<List<ContactMessage>> list(@RequestParam(required = false) ContactStatus status) {
        return ResponseEntity.ok(contactService.list(status));
    }

    @PutMapping("/admin/messages/{messageId}/resolve")
    @Operation(summary = "Mark a message resolved")
    public ResponseEntity<ContactMessage> resolve(@PathVariable String messageId, @AuthenticationPrincipal String adminId) {
        return ResponseEntity.ok(contactService.resolve(messageId, adminId));
    }
}
