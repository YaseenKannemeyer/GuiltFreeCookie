package za.ac.cput.guiltfreecookie.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import za.ac.cput.guiltfreecookie.domain.Admin;
import za.ac.cput.guiltfreecookie.domain.Customer;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "dGVzdC1vbmx5LXNlY3JldC10aGF0LWlzLTMyLWJ5dGVzISE=";
    private final JwtService jwtService = new JwtService(SECRET, 60);

    private Claims claimsFor(Admin admin) {
        return jwtService.parse(jwtService.generateToken(admin)).orElseThrow();
    }

    private Admin.Builder admin() {
        return new Admin.Builder().setAdminId("AD001").setEmail("jane@guiltfreecookie.com").setActive(true);
    }

    @Test
    void acceptsATokenForAnActiveAdmin() {
        Admin admin = admin().build();

        assertTrue(JwtAuthenticationFilter.isStillValid(admin, claimsFor(admin)));
    }

    @Test
    void rejectsATokenOnceTheAdminIsDeactivated() {
        Claims claims = claimsFor(admin().build());

        assertFalse(JwtAuthenticationFilter.isStillValid(admin().setActive(false).build(), claims));
    }

    @Test
    void rejectsATokenIssuedBeforeThePasswordChangedEvenInTheSameSecond() {
        Claims claims = claimsFor(admin().build());
        Admin afterChange = admin().setTokenVersion(1).build();

        assertFalse(JwtAuthenticationFilter.isStillValid(afterChange, claims));
    }

    @Test
    void acceptsATokenIssuedAfterThePasswordChanged() {
        Admin afterChange = admin().setTokenVersion(3).build();

        assertTrue(JwtAuthenticationFilter.isStillValid(afterChange, claimsFor(afterChange)));
    }

    @Test
    void tokensFromBeforeVersioningCountAsVersionZero() {
        String legacy = Jwts.builder()
                .subject("AD001")
                .claim("email", "jane@guiltfreecookie.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
        Claims claims = jwtService.parse(legacy).orElseThrow();

        assertTrue(JwtAuthenticationFilter.isStillValid(admin().build(), claims));
        assertFalse(JwtAuthenticationFilter.isStillValid(admin().setTokenVersion(1).build(), claims));
    }

    @Test
    void rejectsTamperedAndForeignTokens() {
        String token = jwtService.generateToken(admin().build());
        JwtService otherServer = new JwtService("b3RoZXItc2VydmVyLXNlY3JldC10aGF0LWlzLTMyLWJ5dGVz", 60);

        assertTrue(jwtService.parse(token).isPresent());
        assertFalse(jwtService.parse(token.substring(0, token.length() - 2) + "xx").isPresent());
        assertFalse(otherServer.parse(token).isPresent());
        assertFalse(jwtService.parse("not-a-jwt").isPresent());
    }

    private Customer.Builder customer() {
        return new Customer.Builder().setCustomerEmail("thandi@example.co.za").setPasswordHash("$2a$10$hash");
    }

    @Test
    void tokensCarryTheirRole() {
        Claims adminClaims = claimsFor(admin().build());
        Claims customerClaims = jwtService.parse(jwtService.generateToken(customer().build())).orElseThrow();

        assertEquals(JwtService.ROLE_ADMIN, adminClaims.get(JwtService.ROLE_CLAIM));
        assertEquals(JwtService.ROLE_CUSTOMER, customerClaims.get(JwtService.ROLE_CLAIM));
        assertEquals("thandi@example.co.za", customerClaims.getSubject());
    }

    @Test
    void acceptsACustomerTokenUntilThePasswordChanges() {
        Customer thandi = customer().build();
        Claims claims = jwtService.parse(jwtService.generateToken(thandi)).orElseThrow();

        assertTrue(JwtAuthenticationFilter.isStillValid(thandi, claims));
        assertFalse(JwtAuthenticationFilter.isStillValid(customer().setTokenVersion(1).build(), claims));
    }

    @Test
    void rejectsACustomerTokenWhenTheAccountHasNoPassword() {
        Claims claims = jwtService.parse(jwtService.generateToken(customer().build())).orElseThrow();

        assertFalse(JwtAuthenticationFilter.isStillValid(customer().setPasswordHash(null).build(), claims));
    }
}
