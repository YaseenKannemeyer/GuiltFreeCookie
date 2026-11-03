package za.ac.cput.guiltfreecookie.controller;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.EmailDeliveryRepository;
import java.io.ByteArrayInputStream;

@RestController
@Profile("(demo & !prod) | showcase")
@RequestMapping("/api/v1/admin/email-deliveries")
public class DemoInboxController {
    private final EmailDeliveryRepository deliveries;
    private final JavaMailSender sender;
    public DemoInboxController(EmailDeliveryRepository deliveries, JavaMailSender sender) {
        this.deliveries = deliveries;
        this.sender = sender;
    }
    @GetMapping(value = "/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(@PathVariable String id) throws Exception {
        var delivery = deliveries.findById(id).orElseThrow(() -> new NotFoundException("Email not found"));
        if (delivery.getPayload() == null) throw new NotFoundException("This email's content has been cleared");
        var message = sender.createMimeMessage(new ByteArrayInputStream(delivery.getPayload()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(html(message));
    }
    private String html(Part part) throws Exception {
        if (part.isMimeType("text/html")) return (String) part.getContent();
        if (part.isMimeType("multipart/*")) {
            var parts = (Multipart) part.getContent();
            for (int i = 0; i < parts.getCount(); i++) {
                var result = html(parts.getBodyPart(i));
                if (!result.isEmpty()) return result;
            }
        }
        return "";
    }
}
