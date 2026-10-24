package za.ac.cput.guiltfreecookie.dto.engagement;

public record ContactRequest(
    String name,
    String email,
    String phone,
    String subject,
    String message,
    String website
) {}
