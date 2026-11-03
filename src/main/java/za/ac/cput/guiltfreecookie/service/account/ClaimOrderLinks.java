package za.ac.cput.guiltfreecookie.service.account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

// "email.expiry.signature", each part base64url. Valid for 24 hours, and only for the address it was sent to.
@Component
public class ClaimOrderLinks {

    static final Duration VALID_FOR = Duration.ofHours(24);
    private static final int MAX_TOKEN_LENGTH = 1024;

    private final byte[] key;
    private final String frontendBase;
    private final Clock clock;

    @Autowired
    public ClaimOrderLinks(@Value("${app.account.claim-secret}") String secret,
                           @Value("${app.frontend.base-url}") String frontendBase) {
        this(secret, frontendBase, Clock.systemUTC());
    }

    ClaimOrderLinks(String secret, String frontendBase, Clock clock) {
        if (secret.isBlank()) {
            throw new IllegalArgumentException("Account claim secret must not be blank");
        }
        this.key = ("account-order-claim:" + secret).getBytes(StandardCharsets.UTF_8);
        this.frontendBase = frontendBase.replaceAll("/+$", "");
        this.clock = clock;
    }

    public String token(String email) {
        String payload = encode(email.getBytes(StandardCharsets.UTF_8)) + "."
                + clock.instant().plus(VALID_FOR).getEpochSecond();
        return payload + "." + encode(sign(payload));
    }

    public String link(String email) {
        return frontendBase + "/account/claim?token=" + token(email);
    }

    public void verify(String token, String email) {
        try {
            if (token == null || token.length() > MAX_TOKEN_LENGTH) {
                throw new IllegalArgumentException();
            }
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3
                    || !MessageDigest.isEqual(sign(parts[0] + "." + parts[1]), Base64.getUrlDecoder().decode(parts[2]))) {
                throw new IllegalArgumentException();
            }
            String owner = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            if (!owner.equals(email) || Long.parseLong(parts[1]) <= clock.instant().getEpochSecond()) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("This claim link is invalid or has expired. Sign in with the email it was "
                    + "sent to, or request a new link.");
        }
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
