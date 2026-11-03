package za.ac.cput.guiltfreecookie.security;

import org.springframework.security.core.Authentication;

public final class CurrentUser {

    private CurrentUser() {
    }

    // On public routes an anonymous caller's principal is the string "anonymousUser", so check the role, not the principal.
    public static String customerEmail(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_CUSTOMER".equals(authority.getAuthority()))) {
            return null;
        }
        return (String) authentication.getPrincipal();
    }
}
