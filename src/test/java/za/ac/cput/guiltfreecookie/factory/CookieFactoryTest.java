package za.ac.cput.guiltfreecookie.factory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CookieFactoryTest {

    @Test
    @Order(1)
    void testCreateCookieSuccess() {
        Cookie cookie = CookieFactory.createCookie(
                "CK001",
                CookieCategory.LOW_SUGAR,
                "Chocolate Stevia Cookie",
                "Almond flour, Cocoa powder, Stevia, Coconut oil",
                "Tree Nuts",
                25.00
        );
        
        Assertions.assertNotNull(cookie);
        Assertions.assertEquals("CK001", cookie.getCookieId());
        Assertions.assertEquals(CookieCategory.LOW_SUGAR, cookie.getCategory());
        System.out.println(cookie);
    }

	@Test
	@Order(2)
    void testCreateCookieWithEmptyDescription() {
        // Should fail validation and return null
        Cookie cookie = CookieFactory.createCookie(
                "CK002",
                CookieCategory.CLASSIC,
                "", // Empty description
                "Flour, Sugar",
                "Gluten",
                15.00
        );
        
        Assertions.assertNull(cookie);
        System.out.println("Empty description test passed: " + cookie);
    }

	@Test
	@Order(3)
    void testCreateCookieWithInvalidPrice() {
        // Should fail validation and return null because price <= 0
        Cookie cookie = CookieFactory.createCookie(
                "CK003",
                CookieCategory.VEGAN,
                "Vegan Oatmeal Cookie",
                "Oats, Coconut oil",
                "None",
                -5.00
        );
        
        Assertions.assertNull(cookie);
        System.out.println("Invalid price test passed: " + cookie);
    }
}