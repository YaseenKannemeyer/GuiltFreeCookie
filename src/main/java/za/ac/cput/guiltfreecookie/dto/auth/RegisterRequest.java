package za.ac.cput.guiltfreecookie.dto.auth;

public record RegisterRequest(
        String email,
        String password,
        String firstName,
        String middleName,
        String lastName,
        String mobileNumber,
        Boolean newsletterOptIn) {
}
