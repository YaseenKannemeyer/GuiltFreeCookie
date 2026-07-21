package za.ac.cput.guiltfreecookie.factory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;

class CookieFactoryTest {

    @Test
    void testCreateCookie() {
        Cookie result = CookieFactory.createCookie("cookieId", CookieCategory.LOW_SUGAR, "description", "ingredients", "allergies", Double.valueOf(0));
        Assertions.assertEquals(null, result);
    }
}

