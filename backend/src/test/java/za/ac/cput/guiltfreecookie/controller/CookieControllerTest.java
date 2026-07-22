package za.ac.cput.guiltfreecookie.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.service.CookieService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(MockitoExtension.class)
class CookieControllerTest {

    @Mock
    private CookieService cookieService;

    @InjectMocks
    private CookieController cookieController;

    private Cookie createTestCookie() {
        return new Cookie.Builder()
                .setCookieId("CK099")
                .setCategory(CookieCategory.GLUTEN_FREE)
                .setDescription("Gluten Free Coconut Cookie")
                .setIngredients("Coconut flour, Desiccated coconut, Butter")
                .setAllergies("Dairy, Eggs")
                .setPrice(28.50)
                .build();
    }

	@Test
	@Order(1)
    void testCreate() {
        Cookie testCookie = createTestCookie();
        when(cookieService.create(testCookie)).thenReturn(testCookie);
        
        var result = cookieController.create(testCookie);
        
        assertNotNull(result.getBody());
        assertEquals("CK099", result.getBody().getCookieId());
        verify(cookieService).create(testCookie);
        System.out.println(testCookie);
    }

	@Test
	@Order(2)
    void testRead() {
        Cookie testCookie = createTestCookie();
        when(cookieService.read("CK099")).thenReturn(testCookie);
        
        var result = cookieController.read("CK099");
        
        assertNotNull(result.getBody());
        assertEquals("Gluten Free Coconut Cookie", result.getBody().getDescription());
        verify(cookieService).read("CK099");
        System.out.println(testCookie);
    }
}