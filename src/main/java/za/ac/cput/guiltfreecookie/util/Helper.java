package za.ac.cput.guiltfreecookie.util;

import org.apache.commons.validator.routines.EmailValidator;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class Helper {
    private static final String SA_MOBILE_REGEX = "^(\\+27|27|0)[678]\\d{8}$";
    private static final Pattern SA_MOBILE_PATTERN = Pattern.compile(SA_MOBILE_REGEX);
    private static final EmailValidator validator = EmailValidator.getInstance();

    // Control characters other than tab and newline: invisible, and never
    // something a person meant to type into a form.
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\\t\\n]]");

    private static final Pattern SA_POSTAL_CODE_PATTERN = Pattern.compile("^\\d{4}$");
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    public static final int MAX_EMAIL_LENGTH = 254;
    public static final int MAX_SLUG_LENGTH = 80;
    public static final int MIN_PASSWORD_LENGTH = 8;
    // BCrypt only reads the first 72 bytes and Spring Security rejects longer input.
    public static final int MAX_PASSWORD_BYTES = 72;

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty() || email.length() > MAX_EMAIL_LENGTH) {
            return false;
        }
        return validator.isValid(email);
    }

    public static boolean isValidMobileNumber(String mobile) {
        if (mobile == null || mobile.isEmpty()) {
            return false;
        }
        String validNumber = mobile.replaceAll("\\s+", "");
        return SA_MOBILE_PATTERN.matcher(validNumber).matches();
    }

    /**
     * Trims and strips control characters. Text is stored as typed, not
     * HTML-escaped: escaping happens where it's displayed (Vue escapes every
     * {{ }} binding), so escaping here too would corrupt names like "Fish & Co".
     */
    public static String clean(String value) {
        return value == null ? null : CONTROL_CHARS.matcher(value).replaceAll("").trim();
    }

    public static String cleanEmail(String email) {
        String cleaned = clean(email);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    /** Non-blank and at most {@code max} characters. */
    public static boolean isPresent(String value, int max) {
        return value != null && !value.isBlank() && value.length() <= max;
    }

    /** Blank is fine; otherwise at most {@code max} characters. */
    public static boolean isWithin(String value, int max) {
        return value == null || value.length() <= max;
    }

    public static boolean isValidPassword(String password) {
        return password != null
                && password.length() >= MIN_PASSWORD_LENGTH
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES;
    }

    /** "Toasted Almond & Vanilla" becomes "toasted-almond-vanilla". */
    public static String slugify(String text) {
        if (text == null) {
            return "";
        }
        String ascii = DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        String slug = NON_SLUG_CHARS.matcher(ascii.toLowerCase(Locale.ROOT)).replaceAll("-").replaceAll("^-+|-+$", "");
        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
        }
        return slug;
    }

    public static boolean isValidSlug(String slug) {
        return slug != null && slug.length() <= MAX_SLUG_LENGTH && SLUG_PATTERN.matcher(slug).matches();
    }

    public static boolean isValidPostalCode(String postalCode) {
        return postalCode != null && SA_POSTAL_CODE_PATTERN.matcher(postalCode.trim()).matches();
    }
}
