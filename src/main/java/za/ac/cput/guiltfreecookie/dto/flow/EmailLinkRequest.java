package za.ac.cput.guiltfreecookie.dto.flow;

// The address and signed token from an email link.
public record EmailLinkRequest(String email, String token) {
}
