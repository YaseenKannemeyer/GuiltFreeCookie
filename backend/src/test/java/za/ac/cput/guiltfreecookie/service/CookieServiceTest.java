package za.ac.cput.guiltfreecookie.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(MockitoExtension.class)
class CookieServiceTest {

    @Mock
    private CookieRepository cookieRepository;

    @InjectMocks
    private CookieService cookieService;

    private Cookie createTestCookie() {
        return new Cookie.Builder()
                .setCookieId("CK001")
                .setCategory(CookieCategory.HIGH_PROTEIN)
                .setDescription("Protein Chocolate Cookie")
                .setIngredients("Whey Protein, Cocoa Powder, Peanut Butter")
                .setAllergies("Dairy, Peanuts")
                .setPrice(35.00)
                .build();
    }

    @Test
    @Order(1)
    void testRead() {
        Cookie testCookie = createTestCookie();
        Mockito.when(cookieRepository.findById("CK001")).thenReturn(Optional.of(testCookie));

        Cookie result = cookieService.read("CK001");

        assertNotNull(result);
        assertEquals("Protein Chocolate Cookie", result.getDescription());
        Mockito.verify(cookieRepository).findById("CK001");
        System.out.println(result);
    }

    @Test
    @Order(2)
    void testCreate() {
        Cookie testCookie = createTestCookie();
        Mockito.when(cookieRepository.save(testCookie)).thenReturn(testCookie);

        Cookie result = cookieService.create(testCookie);

        assertNotNull(result);
        assertEquals("CK001", result.getCookieId());
        Mockito.verify(cookieRepository).save(testCookie);
        System.out.println(result);
    }
}