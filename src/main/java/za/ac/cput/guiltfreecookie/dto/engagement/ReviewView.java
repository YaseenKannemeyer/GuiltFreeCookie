package za.ac.cput.guiltfreecookie.dto.engagement;

public record ReviewView(
    String id,
    String cookieId,
    Integer rating,
    String title,
    String body,
    String authorName,
    boolean verifiedBuyer,
    String status,
    java.time.LocalDateTime createdAt,
    java.time.LocalDateTime moderatedAt
) {}
