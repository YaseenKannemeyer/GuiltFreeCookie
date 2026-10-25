package za.ac.cput.guiltfreecookie.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.domain.NewsletterSubscriber;
import za.ac.cput.guiltfreecookie.service.NewsletterService;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.util.List;

@RestController
@RequestMapping("/newsletter")
public class NewsletterController {

    public record SubscribeRequest(String email) {
    }

    private final NewsletterService newsletterService;

    public NewsletterController(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    // Public. Answers the same whether or not the email was already on the
    // list, so the form can't be used to check who has subscribed.
    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody SubscribeRequest request) {
        String email = request == null ? null : Helper.cleanEmail(request.email());
        if (!Helper.isValidEmail(email)) {
            return ResponseEntity.badRequest().build();
        }
        newsletterService.subscribe(email, "footer");
        return ResponseEntity.noContent().build();
    }

    // Admin-only (see SecurityConfig): the list to send campaigns to.
    @GetMapping("/subscribers")
    public ResponseEntity<List<NewsletterSubscriber>> getAll() {
        return ResponseEntity.ok(newsletterService.getAll());
    }
}
