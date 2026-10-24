package za.ac.cput.guiltfreecookie.controller.engagement;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.domain.*;
import za.ac.cput.guiltfreecookie.dto.engagement.*;
import za.ac.cput.guiltfreecookie.service.engagement.*;

@RestController
@RequestMapping("/api/v1")
@Tag(
    name = "Engagement",
    description = "Contact, site information and moderated customer reviews"
)
public class EngagementController {

    private final ContactService contacts;
    private final SiteInfoService site;
    private final ReviewService reviews;

    public EngagementController(
        ContactService contacts,
        SiteInfoService site,
        ReviewService reviews
    ) {
        this.contacts = contacts;
        this.site = site;
        this.reviews = reviews;
    }

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Send a contact message")
    public void contact(@RequestBody ContactRequest request) {
        contacts.submit(request);
    }

    @GetMapping("/site/info")
    @Operation(
        summary = "Public shop information and approved review statistics"
    )
    public Map<String, Object> info() {
        return site.info();
    }

    @GetMapping("/admin/messages")
    @Operation(summary = "List contact messages, newest first")
    public List<ContactMessage> messages(
        @RequestParam(required = false) ContactStatus status
    ) {
        return contacts.list(status);
    }

    @PutMapping("/admin/messages/{id}/resolve")
    @Operation(summary = "Resolve a contact message")
    public ContactMessage resolve(
        @PathVariable String id,
        @AuthenticationPrincipal String admin
    ) {
        return contacts.resolve(id, admin);
    }

    @PostMapping("/me/reviews/{cookieId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Submit one review per customer per cookie for moderation"
    )
    public ReviewView create(
        @PathVariable String cookieId,
        @AuthenticationPrincipal String email,
        @RequestBody ReviewRequest request
    ) {
        return reviews.create(email, cookieId, request);
    }

    @GetMapping("/cookies/{idOrSlug}/reviews")
    @Operation(summary = "Approved reviews and rating histogram")
    public ReviewPage list(
        @PathVariable String idOrSlug,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(defaultValue = "newest") String sort
    ) {
        return reviews.list(idOrSlug, page, size, sort);
    }

    @GetMapping("/reviews/featured")
    @Operation(summary = "Featured approved reviews")
    public List<ReviewView> featured(
        @RequestParam(defaultValue = "6") int limit
    ) {
        return reviews.featured(limit);
    }

    @GetMapping("/admin/reviews")
    @Operation(summary = "List reviews for moderation")
    public List<ReviewView> moderation(
        @RequestParam(required = false) ReviewStatus status
    ) {
        return reviews.moderateList(status);
    }

    @PutMapping("/admin/reviews/{id}/approve")
    @Operation(summary = "Approve a review")
    public ReviewView approve(@PathVariable String id) {
        return reviews.moderate(id, ReviewStatus.APPROVED);
    }

    @PutMapping("/admin/reviews/{id}/reject")
    @Operation(summary = "Reject a review")
    public ReviewView reject(@PathVariable String id) {
        return reviews.moderate(id, ReviewStatus.REJECTED);
    }
}
