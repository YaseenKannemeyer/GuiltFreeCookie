package za.ac.cput.guiltfreecookie.dto.auth;

public record ResetPasswordRequest(String token, String newPassword) {
}
