package za.ac.cput.guiltfreecookie.service.flow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;

// Signed per-address links (unsubscribe), plus the tracked-link and open-pixel URLs for a send.
@Component
public class MarketingLinks {

    private final byte[] key;
    private final String publicBase;
    private final String frontendBase;

    public MarketingLinks(@Value("${app.email.link-secret}") String secret,
                          @Value("${app.public-base-url}") String publicBase,
                          @Value("${app.frontend.base-url}") String frontendBase) {
        this.key = ("marketing-links:" + secret).getBytes(StandardCharsets.UTF_8);
        this.publicBase = publicBase.replaceAll("/+$", "");
        this.frontendBase = frontendBase.replaceAll("/+$", "");
    }

    public String token(String email) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] digest = mac.doFinal(email.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean matches(String email, String token) {
        return email != null && token != null && MessageDigest.isEqual(
                token(email).getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }

    public String unsubscribePage(String email) {
        return frontendBase + "/email/unsubscribe?e=" + encode(email) + "&t=" + token(email);
    }

    // For the List-Unsubscribe header: mail apps POST here directly (RFC 8058 one-click).
    public String oneClickUnsubscribe(String email) {
        return publicBase + "/api/v1/email/unsubscribe/one-click?e=" + encode(email) + "&t=" + token(email);
    }

    public String openPixel(String sendId) {
        return publicBase + "/api/v1/email/o/" + sendId + ".gif";
    }

    public String tracked(String sendId, String target) {
        return publicBase + "/api/v1/email/c/" + sendId + "?u=" + encode(target);
    }

    public String frontend(String path) {
        return frontendBase + path;
    }

    // Only our own storefront, so the click redirect can't be used to bounce people elsewhere.
    public boolean isStorefront(String url) {
        if (url == null) {
            return false;
        }
        try {
            URI target = URI.create(url);
            URI base = URI.create(frontendBase);
            return target.getScheme() != null && target.getScheme().equalsIgnoreCase(base.getScheme())
                    && target.getHost() != null && target.getHost().equalsIgnoreCase(base.getHost())
                    && target.getPort() == base.getPort()
                    && target.getRawUserInfo() == null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
