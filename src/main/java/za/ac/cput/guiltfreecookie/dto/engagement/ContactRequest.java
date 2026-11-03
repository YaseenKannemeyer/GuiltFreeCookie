package za.ac.cput.guiltfreecookie.dto.engagement;

// website is a honeypot: people never see it, bots fill it in.
public record ContactRequest(String name, String email, String phone, String subject, String message, String website) {
}
