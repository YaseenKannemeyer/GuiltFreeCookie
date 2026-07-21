package za.ac.cput.guiltfreecookie.factory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
class CookieFactoryTest {

    @Test
    void testCreateCookie(){
        za.ac.cput.guiltfreecookie.domain.Cookie result = CookieFactory.createCookie("cookieId", za.ac.cput.guiltfreecookie.domain.CookieCategory.LOW_SUGAR, "description", "ingredients", "allergies", Double.valueOf(0));
        Assertions.assertEquals(null, result);
    }
}

//Generated with love by TestMe :) Please raise issues & feature requests at: https://weirddev.com/forum#!/testme