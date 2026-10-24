package za.ac.cput.guiltfreecookie.service.cart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import za.ac.cput.guiltfreecookie.domain.CartStatus;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.domain.DiscountCode;
import za.ac.cput.guiltfreecookie.domain.DiscountType;
import za.ac.cput.guiltfreecookie.dto.cart.AddItemRequest;
import za.ac.cput.guiltfreecookie.dto.cart.CartLine;
import za.ac.cput.guiltfreecookie.dto.cart.CartView;
import za.ac.cput.guiltfreecookie.dto.cart.LineIssue;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CookieVariantRepository;
import za.ac.cput.guiltfreecookie.repository.DiscountCodeRepository;
import za.ac.cput.guiltfreecookie.service.discount.DiscountService;
import za.ac.cput.guiltfreecookie.service.shipping.RuleBasedShippingProvider;
import za.ac.cput.guiltfreecookie.service.shipping.ShippingSettingsService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Real queries on H2: locking, stock arithmetic, pricing and merging together.
@DataJpaTest
@Import({ShopCartService.class, PricingService.class, DiscountService.class,
        RuleBasedShippingProvider.class, ShippingSettingsService.class})
class ShopCartServiceTest {

    @Autowired
    private ShopCartService carts;
    @Autowired
    private CookieRepository cookieRepository;
    @Autowired
    private CookieVariantRepository variantRepository;
    @Autowired
    private DiscountCodeRepository discountRepository;
    @Autowired
    private CartRepository cartRepository;

    private Cookie chunk;
    private String single;
    private String boxOfSix;

    @BeforeEach
    void setUp() {
        chunk = cookieRepository.save(new Cookie.Builder().setDescription("Double Chocolate Chunk").setSlug("chunk")
                .setCategory(CookieCategory.LOW_SUGAR).setPrice(30.0).setStockQuantity(10).setIngredients("Cocoa")
                .setAllergies("None").build());
        single = variant(chunk, "Single", 1, null);
        boxOfSix = variant(chunk, "Box of 6", 6, "170");
    }

    private String variant(Cookie cookie, String label, int units, String price) {
        return variantRepository.save(new CookieVariant.Builder().setCookieId(cookie.getCookieId()).setLabel(label)
                .setUnitsPerPack(units).setPrice(price == null ? null : new BigDecimal(price)).build()).getVariantId();
    }

    private CartView add(String cartId, String variantId, int quantity) {
        return carts.addItem(cartId, new AddItemRequest(variantId, quantity));
    }

    @Test
    void addingPricesTheCartFromTheDatabase() {
        String cartId = carts.create(null).cartId();

        CartView cart = add(cartId, boxOfSix, 1);
        cart = add(cartId, single, 2);

        assertEquals(2, cart.items().size());
        assertEquals(new BigDecimal("230.00"), cart.subtotal());
        assertEquals(new BigDecimal("230.00"), cart.total());
        assertEquals(3, cart.itemCount());
        assertEquals(8, cart.cookieCount());
        assertTrue(cart.canCheckout());
        assertEquals(230.0, cartRepository.findById(cartId).orElseThrow().getTotalPrice());
    }

    @Test
    void addingTheSamePackAgainIncreasesItsQuantity() {
        String cartId = carts.create(null).cartId();

        add(cartId, single, 1);
        CartView cart = add(cartId, single, 2);

        assertEquals(1, cart.items().size());
        assertEquals(3, cart.items().getFirst().quantity());
    }

    @Test
    void stockIsCheckedAcrossPackSizesOfTheSameCookie() {
        String cartId = carts.create(null).cartId();
        add(cartId, boxOfSix, 1);

        ConflictException tooMany = assertThrows(ConflictException.class, () -> add(cartId, single, 5));
        assertEquals("Sorry, you can only add 4 more Double Chocolate Chunk cookies", tooMany.getMessage());

        CartView cart = add(cartId, single, 4);
        assertEquals(4, cart.items().get(1).maxQuantity());

        ConflictException full = assertThrows(ConflictException.class, () -> add(cartId, single, 1));
        assertEquals("You already have all the Double Chocolate Chunk cookies we have in stock", full.getMessage());
    }

    @Test
    void anEmptyCartIsToldHowManyAreLeftOrThatItIsSoldOut() {
        String cartId = carts.create(null).cartId();

        assertEquals("Sorry, we only have 10 Double Chocolate Chunk cookies left",
                assertThrows(ConflictException.class, () -> add(cartId, boxOfSix, 2)).getMessage());

        cookieRepository.setStock(chunk.getCookieId(), 0);
        assertEquals("Sorry, Double Chocolate Chunk is sold out",
                assertThrows(ConflictException.class, () -> add(cartId, single, 1)).getMessage());
    }

    @Test
    void quantityIsLimitedAndZeroRemoves() {
        String cartId = carts.create(null).cartId();
        CartLine line = add(cartId, single, 1).items().getFirst();

        assertThrows(BadRequestException.class, () -> add(cartId, single, 0));
        assertThrows(BadRequestException.class, () -> carts.updateQuantity(cartId, line.cartItemId(), 51));
        assertEquals(0, carts.updateQuantity(cartId, line.cartItemId(), 0).items().size());
    }

    @Test
    void aHiddenPackOrArchivedCookieCantBeAdded() {
        String cartId = carts.create(null).cartId();
        CookieVariant box = variantRepository.findById(boxOfSix).orElseThrow();
        variantRepository.save(new CookieVariant.Builder().copy(box).setActive(false).build());

        assertThrows(NotFoundException.class, () -> add(cartId, boxOfSix, 1));
        assertThrows(NotFoundException.class, () -> add(cartId, "made-up", 1));
    }

    @Test
    void linesThatGoBadAfterAddingBlockCheckout() {
        String cartId = carts.create(null).cartId();
        add(cartId, single, 5);
        cookieRepository.setStock(chunk.getCookieId(), 3);

        CartView cart = carts.get(cartId);

        assertEquals(LineIssue.NOT_ENOUGH_STOCK, cart.items().getFirst().issue());
        assertEquals(3, cart.items().getFirst().maxQuantity());
        assertFalse(cart.canCheckout());

        cookieRepository.save(new Cookie.Builder().copy(cookieRepository.findById(chunk.getCookieId()).orElseThrow()).setArchived(true).build());
        cart = carts.get(cartId);
        assertEquals(LineIssue.UNAVAILABLE, cart.items().getFirst().issue());
        assertEquals(new BigDecimal("0.00"), cart.subtotal());
    }

    @Test
    void aDiscountIsAppliedAndReEvaluatedAsTheCartChanges() {
        discountRepository.save(new DiscountCode.Builder().setCode("TREAT10").setType(DiscountType.PERCENT)
                .setValue(BigDecimal.TEN).setMinSubtotal(new BigDecimal("100")).setCreatedAt(LocalDateTime.now()).build());
        String cartId = carts.create(null).cartId();
        CartLine line = add(cartId, single, 5).items().getFirst();

        CartView cart = carts.applyDiscount(cartId, "treat10");
        assertEquals(new BigDecimal("15.00"), cart.discountTotal());
        assertEquals(new BigDecimal("135.00"), cart.total());

        cart = carts.updateQuantity(cartId, line.cartItemId(), 2);
        assertFalse(cart.discount().applied());
        assertEquals("Spend R100.00 or more to use this code", cart.discount().message());
        assertEquals(new BigDecimal("60.00"), cart.total());
    }

    @Test
    void aBadCodeIsRejectedWithAReason() {
        String cartId = carts.create(null).cartId();

        BadRequestException error = assertThrows(BadRequestException.class, () -> carts.applyDiscount(cartId, "NOPE"));
        assertEquals("This code isn't valid", error.getMessage());
    }

    @Test
    void aSignedInCustomerKeepsOneCart() {
        String first = carts.create("thandi@example.co.za").cartId();

        assertEquals(first, carts.create("thandi@example.co.za").cartId());
        assertEquals(first, carts.myCart("thandi@example.co.za").cartId());
        assertThrows(ConflictException.class, () -> carts.setEmail(first, "other@example.co.za"));
    }

    @Test
    void aGuestCanLeaveAnEmail() {
        String cartId = carts.create(null).cartId();

        assertEquals("thandi@example.co.za", carts.setEmail(cartId, " Thandi@Example.co.za ").email());
        assertThrows(BadRequestException.class, () -> carts.setEmail(cartId, "nope"));
    }

    @Test
    void mergeClaimsTheGuestCartWhenTheCustomerHasNone() {
        String guest = carts.create(null).cartId();
        add(guest, single, 2);

        CartView merged = carts.merge("thandi@example.co.za", guest);

        assertEquals(guest, merged.cartId());
        assertTrue(merged.accountOwned());
        assertEquals("thandi@example.co.za", merged.email());
    }

    @Test
    void mergeMovesGuestItemsIntoTheExistingAccountCartWithinStock() {
        String account = carts.create("thandi@example.co.za").cartId();
        add(account, single, 3);
        String guest = carts.create(null).cartId();
        add(guest, single, 2);
        add(guest, boxOfSix, 1);

        CartView merged = carts.merge("thandi@example.co.za", guest);

        assertEquals(account, merged.cartId());
        assertEquals(5, merged.items().stream().filter(l -> l.variantId().equals(single)).findFirst().orElseThrow().quantity());
        assertTrue(merged.items().stream().noneMatch(l -> l.variantId().equals(boxOfSix)));
        assertEquals(CartStatus.CLEARED, cartRepository.findById(guest).orElseThrow().getStatus());
        assertThrows(ConflictException.class, () -> add(guest, single, 1));
    }

    @Test
    void someoneElsesAccountCartCantBeMerged() {
        String theirs = carts.create("owner@example.co.za").cartId();

        assertThrows(NotFoundException.class, () -> carts.merge("thandi@example.co.za", theirs));
    }

    @Test
    void clearingEmptiesItemsAndDropsTheCode() {
        String cartId = carts.create(null).cartId();
        add(cartId, single, 1);

        CartView cart = carts.clear(cartId);

        assertEquals(0, cart.items().size());
        assertNull(cart.discount());
        assertFalse(cart.canCheckout());
    }
}
