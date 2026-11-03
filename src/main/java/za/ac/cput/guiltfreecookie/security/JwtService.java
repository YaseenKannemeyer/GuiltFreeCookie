package za.ac.cput.guiltfreecookie.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import za.ac.cput.guiltfreecookie.domain.Admin;
import za.ac.cput.guiltfreecookie.domain.Customer;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtService {

    // The admin's token version when the token was issued (see Admin.tokenVersion).
    public static final String VERSION_CLAIM = "ver";
    public static final String ROLE_CLAIM = "role";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set. Generate one with: openssl rand -base64 32");
        }
        // Rejects keys shorter than 256 bits.
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret.trim()));
        this.expirationMillis = expirationMinutes * 60_000;
    }

    public String generateToken(Admin admin) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(admin.getAdminId())
                .claim("email", admin.getEmail())
                .claim(VERSION_CLAIM, admin.getTokenVersion())
                .claim(ROLE_CLAIM, ROLE_ADMIN)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String generateToken(Customer customer) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(customer.getCustomerEmail())
                .claim(VERSION_CLAIM, customer.getTokenVersion())
                .claim(ROLE_CLAIM, ROLE_CUSTOMER)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** The token's claims if its signature checks out and it hasn't expired. */
    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
