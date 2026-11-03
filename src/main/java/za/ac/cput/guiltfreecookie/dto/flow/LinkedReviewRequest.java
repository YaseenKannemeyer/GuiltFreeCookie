package za.ac.cput.guiltfreecookie.dto.flow;

public record LinkedReviewRequest(
        String orderNumber,
        String token,
        String cookieId,
        Integer rating,
        String title,
        String body,
        String authorName) {
}
