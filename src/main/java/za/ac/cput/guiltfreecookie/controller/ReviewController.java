package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.domain.ReviewStatus;
import za.ac.cput.guiltfreecookie.dto.engagement.ReviewPage;
import za.ac.cput.guiltfreecookie.dto.engagement.ReviewRequest;
import za.ac.cput.guiltfreecookie.dto.engagement.ReviewView;
import za.ac.cput.guiltfreecookie.service.engagement.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reviews", description = "Customer reviews: written by signed-in customers, shown once an admin approves them.")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/me/reviews/{cookieId}")
    @Operation(summary = "Write a review", description = "Customer token. One per cookie; it waits for moderation.")
    public ResponseEntity<ReviewView> create(
            @PathVariable String cookieId,
            @AuthenticationPrincipal String email,
            @RequestBody ReviewRequest request) {
        return new ResponseEntity<>(reviewService.create(email, cookieId, request), HttpStatus.CREATED);
    }

    @GetMapping("/cookies/{idOrSlug}/reviews")
    @Operation(summary = "Approved reviews with the rating histogram")
    public ResponseEntity<ReviewPage> list(
            @PathVariable String idOrSlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "newest") String sort) {
        return ResponseEntity.ok(reviewService.list(idOrSlug, page, size, sort));
    }

    @GetMapping("/reviews/featured")
    @Operation(summary = "Homepage testimonials: approved 4 and 5 star reviews")
    public ResponseEntity<List<ReviewView>> featured(@RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(reviewService.featured(limit));
    }

    @GetMapping("/admin/reviews")
    @Operation(summary = "Reviews to moderate, newest first")
    public ResponseEntity<List<ReviewView>> moderationList(@RequestParam(required = false) ReviewStatus status) {
        return ResponseEntity.ok(reviewService.moderationList(status));
    }

    @PutMapping("/admin/reviews/{reviewId}/approve")
    public ResponseEntity<ReviewView> approve(@PathVariable String reviewId) {
        return ResponseEntity.ok(reviewService.moderate(reviewId, ReviewStatus.APPROVED));
    }

    @PutMapping("/admin/reviews/{reviewId}/reject")
    public ResponseEntity<ReviewView> reject(@PathVariable String reviewId) {
        return ResponseEntity.ok(reviewService.moderate(reviewId, ReviewStatus.REJECTED));
    }
}
