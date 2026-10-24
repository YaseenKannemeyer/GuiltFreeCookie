package za.ac.cput.guiltfreecookie.service.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.CartStatus;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.DiscountCode;
import za.ac.cput.guiltfreecookie.domain.DiscountType;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.Province;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.dto.cart.AddItemRequest;
import za.ac.cput.guiltfreecookie.dto.order.AddressRequest;
import za.ac.cput.guiltfreecookie.dto.order.CheckoutRequest;
import za.ac.cput.guiltfreecookie.dto.order.CheckoutResponse;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CookieVariantRepository;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.DiscountCodeRepository;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;
import za.ac.cput.guiltfreecookie.service.cart.ShopCartService;
import za.ac.cput.guiltfreecookie.service.payment.PayFastSignature;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Checkout end to end on H2: pricing, stock holding, order creation and the PayFast hand-over.
@SpringBootTest
@Transactional
class CheckoutFlowTest {

    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private ShopCartService cartService;
    @Autowired
    private OrderLinks orderLinks;
    @Autowired
    private CookieRepository cookieRepository;
    @Autowired
    private CookieVariantRepository variantRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CustomerOrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private DiscountCodeRepository discountRepository;

    private Cookie chunk;
    private Cookie lemon;
    private String chunkBox;
    private String lemonSingle;

    @BeforeEach
    void setUp() {
        chunk = cookie("Double Chocolate Chunk", 30.0, 20);
        lemon = cookie("Lemon Almond", 25.0, 5);
        chunkBox = variant(chunk, "Box of 6", 6, "170");
        lemonSingle = variant(lemon, "Single", 1, null);
    }

    private Cookie cookie(String name, double price, int stock) {
        return cookieRepository.save(new Cookie.Builder().setDescription(name).setSlug(name.toLowerCase().replace(' ', '-'))
                .setCategory(CookieCategory.LOW_SUGAR).setPrice(price).setStockQuantity(stock)
                .setIngredients("Cocoa").setAllergies("None").build());
    }

    private String variant(Cookie cookie, String label, int units, String price) {
        return variantRepository.save(new CookieVariant.Builder().setCookieId(cookie.getCookieId()).setLabel(label)
                .setUnitsPerPack(units).setPrice(price == null ? null : new BigDecimal(price)).build()).getVariantId();
    }

    private String cartWith(String variantId, int quantity) {
        String cartId = cartService.create(null).cartId();
        cartService.addItem(cartId, new AddItemRequest(variantId, quantity));
        return cartId;
    }

    private AddressRequest address() {
        return new AddressRequest("12", "Bakery Lane", null, "Strandfontein", "Cape Town", Province.WESTERN_CAPE, "7798");
    }

    private CheckoutRequest courier(String cartId) {
        return new CheckoutRequest(cartId, "Thandi@Example.co.za", "Thandi", "Mokoena", "082 123 4567",
                ShippingMethod.COURIER, address(), null, "Leave at the gate", true);
    }

    private CheckoutRequest pickup(String cartId) {
        return new CheckoutRequest(cartId, "thandi@example.co.za", "Thandi", "Mokoena", null,
                ShippingMethod.PICKUP, null, null, null, null);
    }

    private int stock(Cookie cookie) {
        return cookieRepository.findById(cookie.getCookieId()).orElseThrow().getStockQuantity();
    }

    @Test
    void aGuestCourierCheckoutHoldsStockAndHandsOverToPayFast() {
        String cartId = cartWith(chunkBox, 2);

        CheckoutResponse response = checkoutService.checkout(courier(cartId), null);

        CustomerOrder order = orderRepository.findByOrderNumber(response.orderNumber()).orElseThrow();
        assertTrue(response.orderNumber().matches("GFC-\\d{6}-[2-9A-Z]{4}"));
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        assertEquals("thandi@example.co.za", order.getCustomerEmail());
        assertEquals(new BigDecimal("340.00"), order.getSubtotal());
        assertEquals(new BigDecimal("80.00"), order.getShippingTotal());
        assertEquals(new BigDecimal("420.00"), order.getTotal());
        assertEquals("7798", order.getAddress().getPostalCode());
        assertEquals("Leave at the gate", order.getCustomerNote());
        assertTrue(order.getReservedUntil().isAfter(LocalDateTime.now().plusMinutes(29)));
        assertEquals(8, stock(chunk));
        assertEquals(1, orderItemRepository.findByOrderId(order.getOrderId()).size());
        assertEquals(CartStatus.CHECKED_OUT, cartRepository.findById(cartId).orElseThrow().getStatus());

        assertTrue(orderLinks.matches(order, response.accessToken()));
        Map<String, String> fields = response.payment().fields();
        assertEquals("https://sandbox.payfast.co.za/eng/process", response.payment().actionUrl());
        assertEquals("420.00", fields.get("amount"));
        assertEquals(response.orderNumber(), fields.get("m_payment_id"));
        assertEquals("0821234567", fields.get("cell_number"));
        Map<String, String> signed = new LinkedHashMap<>(fields);
        String signature = signed.remove("signature");
        assertEquals(PayFastSignature.sign(PayFastSignature.paramString(signed, false), "jt7NOE43FZPn"), signature);
    }

    @Test
    void deliveryIsFreeOverTheThreshold() {
        String cartId = cartWith(chunkBox, 3);

        CheckoutResponse response = checkoutService.checkout(courier(cartId), null);

        CustomerOrder order = orderRepository.findByOrderNumber(response.orderNumber()).orElseThrow();
        assertEquals(new BigDecimal("0.00"), order.getShippingTotal());
        assertEquals(new BigDecimal("510.00"), order.getTotal());
    }

    @Test
    void pickupNeedsNoAddressOrPhone() {
        CheckoutResponse response = checkoutService.checkout(pickup(cartWith(lemonSingle, 2)), null);

        CustomerOrder order = orderRepository.findByOrderNumber(response.orderNumber()).orElseThrow();
        assertEquals(new BigDecimal("50.00"), order.getTotal());
        assertNull(order.getAddress());
        assertNull(order.getPhone());
    }

    @Test
    void deliveryDetailsAreValidated() {
        String cartId = cartWith(lemonSingle, 1);

        assertThrows(BadRequestException.class, () -> checkoutService.checkout(new CheckoutRequest(cartId, "t@example.co.za",
                "Thandi", "Mokoena", "0821234567", ShippingMethod.COURIER, null, null, null, null), null));
        assertThrows(BadRequestException.class, () -> checkoutService.checkout(new CheckoutRequest(cartId, "t@example.co.za",
                "Thandi", "Mokoena", "0821234567", ShippingMethod.COURIER,
                new AddressRequest("12", "Bakery Lane", null, "Strandfontein", "Cape Town", Province.WESTERN_CAPE, "779"),
                null, null, null), null));
        assertThrows(BadRequestException.class, () -> checkoutService.checkout(new CheckoutRequest(cartId, "t@example.co.za",
                "Thandi", "Mokoena", "0821234567", ShippingMethod.PUDO, null, " ", null, null), null));
        assertThrows(BadRequestException.class, () -> checkoutService.checkout(new CheckoutRequest(cartId, "t@example.co.za",
                "Thandi", "Mokoena", null, ShippingMethod.COURIER, address(), null, null, null), null));
        assertThrows(BadRequestException.class, () -> checkoutService.checkout(new CheckoutRequest(cartId, "nope",
                "Thandi", "Mokoena", null, ShippingMethod.PICKUP, null, null, null, null), null));
        assertEquals(5, stock(lemon));
    }

    @Test
    void anItemThatSoldOutBlocksCheckoutWithoutHoldingAnyStock() {
        String cartId = cartWith(chunkBox, 1);
        cartService.addItem(cartId, new AddItemRequest(lemonSingle, 5));
        cookieRepository.setStock(lemon.getCookieId(), 2);

        assertThrows(ConflictException.class, () -> checkoutService.checkout(pickup(cartId), null));
        assertEquals(20, stock(chunk));
        assertEquals(CartStatus.ACTIVE, cartRepository.findById(cartId).orElseThrow().getStatus());
    }

    @Test
    void aCartCantBeCheckedOutTwice() {
        String cartId = cartWith(lemonSingle, 1);
        checkoutService.checkout(pickup(cartId), null);

        assertThrows(ConflictException.class, () -> checkoutService.checkout(pickup(cartId), null));
    }

    @Test
    void aCodeThatNoLongerAppliesMustBeRemovedFirst() {
        discountRepository.save(new DiscountCode.Builder().setCode("BIG").setType(DiscountType.FIXED)
                .setValue(BigDecimal.TEN).setMinSubtotal(new BigDecimal("100")).build());
        String cartId = cartWith(chunkBox, 1);
        cartService.applyDiscount(cartId, "BIG");
        cookieRepository.save(new Cookie.Builder().copy(cookieRepository.findById(chunk.getCookieId()).orElseThrow())
                .setPrice(10.0).build());
        variantRepository.save(new CookieVariant.Builder().copy(variantRepository.findById(chunkBox).orElseThrow())
                .setPrice(new BigDecimal("50")).build());

        ConflictException error = assertThrows(ConflictException.class, () -> checkoutService.checkout(pickup(cartId), null));
        assertTrue(error.getMessage().startsWith("Spend R100.00"));
    }

    @Test
    void aOnePerEmailCodeCantBeUsedTwice() {
        discountRepository.save(new DiscountCode.Builder().setCode("ONCE").setType(DiscountType.PERCENT)
                .setValue(BigDecimal.TEN).setOnePerEmail(true).build());
        String first = cartWith(lemonSingle, 1);
        cartService.applyDiscount(first, "ONCE");
        CheckoutResponse paid = checkoutService.checkout(pickup(first), null);
        orderRepository.save(new CustomerOrder.Builder()
                .copy(orderRepository.findByOrderNumber(paid.orderNumber()).orElseThrow()).setStatus(OrderStatus.PAID).build());

        String second = cartWith(lemonSingle, 1);
        cartService.applyDiscount(second, "ONCE");
        ConflictException error = assertThrows(ConflictException.class, () -> checkoutService.checkout(pickup(second), null));
        assertEquals("You've already used code ONCE", error.getMessage());
    }

    @Test
    void aSignedInCustomerChecksOutWithTheirAccountEmailOnly() {
        String cartId = cartService.create("member@example.co.za").cartId();
        cartService.addItem(cartId, new AddItemRequest(lemonSingle, 1));

        assertThrows(NotFoundException.class, () -> checkoutService.checkout(pickup(cartId), null));
        assertThrows(NotFoundException.class, () -> checkoutService.checkout(pickup(cartId), "other@example.co.za"));

        CheckoutResponse response = checkoutService.checkout(pickup(cartId), "member@example.co.za");
        CustomerOrder order = orderRepository.findByOrderNumber(response.orderNumber()).orElseThrow();
        assertEquals("member@example.co.za", order.getCustomerEmail());
        assertTrue(order.isAccountOwned());
    }

    @Test
    void anOrderWithNothingToPayIsPaidStraightAway() {
        discountRepository.save(new DiscountCode.Builder().setCode("ALLFREE").setType(DiscountType.PERCENT)
                .setValue(new BigDecimal("100")).build());
        String cartId = cartWith(lemonSingle, 1);
        cartService.applyDiscount(cartId, "ALLFREE");

        CheckoutResponse response = checkoutService.checkout(pickup(cartId), null);

        assertEquals(OrderStatus.PAID, response.status());
        assertNull(response.payment());
        assertNotNull(orderRepository.findByOrderNumber(response.orderNumber()).orElseThrow().getPaidAt());
    }
}
