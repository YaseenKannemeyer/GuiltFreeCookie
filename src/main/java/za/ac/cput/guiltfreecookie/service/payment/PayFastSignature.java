package za.ac.cput.guiltfreecookie.service.payment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.StringJoiner;

// PayFast's MD5 signature: key=urlencode(value) pairs joined with &, in the order given, then the passphrase.
public final class PayFastSignature {

    private PayFastSignature() {
    }

    // Checkout forms skip blank fields; payment notifications sign every field they send.
    public static String paramString(Map<String, String> fields, boolean includeBlank) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> field : fields.entrySet()) {
            String value = field.getValue() == null ? "" : field.getValue().trim();
            if (includeBlank || !value.isEmpty()) {
                joiner.add(field.getKey() + "=" + encode(value));
            }
        }
        return joiner.toString();
    }

    public static String sign(String paramString, String passphrase) {
        String toSign = passphrase == null || passphrase.isBlank()
                ? paramString
                : paramString + "&passphrase=" + encode(passphrase.trim());
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(toSign.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // PHP's urlencode(), which PayFast uses: like Java's URLEncoder except '*' is encoded too.
    static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("*", "%2A");
    }
}
