package za.ac.cput.guiltfreecookie.service.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

// A guest's key to their order page: an HMAC of the order number, so it can be recreated for
// emails at any time without storing it, and can't be guessed without the server secret.
@Component
public class OrderLinks {

    private final byte[] key;
    private final String frontendBase;

    public OrderLinks(@Value("${app.order.link-secret}") String secret,
                      @Value("${app.frontend.base-url}") String frontendBase) {
        this.key = ("order-links:" + secret).getBytes(StandardCharsets.UTF_8);
        this.frontendBase = frontendBase.replaceAll("/+$", "");
    }

    public String token(CustomerOrder order) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] digest = mac.doFinal(order.getOrderNumber().getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean matches(CustomerOrder order, String token) {
        return token != null && MessageDigest.isEqual(
                token(order).getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }

    public String orderPage(CustomerOrder order) {
        return frontendBase + "/order/" + order.getOrderNumber() + "?token=" + token(order);
    }

    public String paymentCancelled(CustomerOrder order) {
        return frontendBase + "/checkout/cancelled?order=" + order.getOrderNumber() + "&token=" + token(order);
    }
}
