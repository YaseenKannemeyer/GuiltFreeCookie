package za.ac.cput.guiltfreecookie.util;

import org.apache.commons.validator.routines.EmailValidator;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;

import java.util.regex.Pattern;

public class Helper {

    private static final String SA_MOBILE_REGEX = "^(\\+27|27|0)[678]\\d{8}$";
    private static final Pattern SA_MOBILE_PATTERN = Pattern.compile(SA_MOBILE_REGEX);
    private static final EmailValidator validator = EmailValidator.getInstance();


    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return validator.isValid(email);
    }

    public static boolean isValidMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.isEmpty()) {
            return false;
        }
        String validNumber = mobileNumber.replaceAll("\\s+|-", "");
        return SA_MOBILE_PATTERN.matcher(validNumber).matches();
    }
}
