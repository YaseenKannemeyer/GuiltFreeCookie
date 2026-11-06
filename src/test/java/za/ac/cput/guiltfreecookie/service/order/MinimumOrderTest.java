package za.ac.cput.guiltfreecookie.service.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.dto.cart.AddItemRequest;
import za.ac.cput.guiltfreecookie.dto.cart.CartView;
import za.ac.cput.guiltfreecookie.dto.order.CheckoutRequest;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CookieVariantRepository;
import za.ac.cput.guiltfreecookie.service.cart.ShopCartService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.order.minimum-subtotal=300")
@Transactional
class MinimumOrderTest {

    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private ShopCartService cartService;
    @Autowired
    private CookieRepository cookieRepository;
    @Autowired
    private CookieVariantRepository variantRepository;

    private String box(String price) {
        Cookie cookie = cookieRepository.save(new Cookie.Builder().setDescription("Oat Crunch").setSlug("oat-crunch")
                .setCategory(CookieCategory.LOW_SUGAR).setPrice(5.0).setStockQuantity(500)
                .setIngredients("Oats").setAllergies("Gluten").build());
        return variantRepository.save(new CookieVariant.Builder().setCookieId(cookie.getCookieId()).setLabel("Box of 24")
                .setUnitsPerPack(24).setPrice(new BigDecimal(price)).build()).getVariantId();
    }

    private CheckoutRequest pickup(String cartId) {
        return new CheckoutRequest(cartId, "thandi@example.co.za", "Thandi", "Mokoena", null,
                ShippingMethod.PICKUP, null, null, null, null);
    }

    @Test
    void aCartUnderTheMinimumCantCheckOut() {
        String cartId = cartService.create(null).cartId();
        CartView cart = cartService.addItem(cartId, new AddItemRequest(box("84.00"), 3));

        assertTrue(cart.belowMinimum());
        assertFalse(cart.canCheckout());
        assertEquals(new BigDecimal("300.00"), cart.minimumOrder());
        ConflictException error = assertThrows(ConflictException.class, () -> checkoutService.checkout(pickup(cartId), null));
        assertEquals("Orders start at R300.00 worth of cookies. Add R48.00 more to check out.", error.getMessage());
    }

    @Test
    void reachingTheMinimumUnlocksCheckout() {
        String cartId = cartService.create(null).cartId();
        CartView cart = cartService.addItem(cartId, new AddItemRequest(box("100.00"), 3));

        assertFalse(cart.belowMinimum());
        assertTrue(cart.canCheckout());
        assertNotNull(checkoutService.checkout(pickup(cartId), null).orderNumber());
    }
}
