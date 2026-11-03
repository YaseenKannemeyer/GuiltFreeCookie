package za.ac.cput.guiltfreecookie.dto.engagement;

import java.time.LocalDateTime;

// The public and admin shape of a review; deliberately leaves out the customer's email.
public record ReviewView(
        String id,
        String cookieId,
        Integer rating,
        String title,
        String body,
        String authorName,
        boolean verifiedBuyer,
        String status,
        LocalDateTime createdAt,
        LocalDateTime moderatedAt) {
}
