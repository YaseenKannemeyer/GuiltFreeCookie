package za.ac.cput.guiltfreecookie.service.order;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
import za.ac.cput.guiltfreecookie.domain.OrderStatusEvent;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.dto.cart.AddItemRequest;
import za.ac.cput.guiltfreecookie.dto.order.AdminOrderView;
import za.ac.cput.guiltfreecookie.dto.order.CheckoutRequest;
import za.ac.cput.guiltfreecookie.dto.order.OrderView;
import za.ac.cput.guiltfreecookie.dto.order.StatusChangeRequest;
import za.ac.cput.guiltfreecookie.dto.order.TrackingRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CookieVariantRepository;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.DiscountCodeRepository;
import za.ac.cput.guiltfreecookie.repository.OrderStatusEventRepository;
import za.ac.cput.guiltfreecookie.service.cart.ShopCartService;
import za.ac.cput.guiltfreecookie.service.engagement.PurchaseVerifier;
import za.ac.cput.guiltfreecookie.service.engagement.SiteInfoService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class OrderLifecycleTest {

    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private AdminOrderService adminOrderService;
    @Autowired
    private ShopCartService cartService;
    @Autowired
    private PurchaseVerifier purchaseVerifier;
    @Autowired
    private SiteInfoService siteInfoService;
    @Autowired
    private CookieRepository cookieRepository;
    @Autowired
    private CookieVariantRepository variantRepository;
    @Autowired
    private CustomerOrderRepository orderRepository;
    @Autowired
    private OrderStatusEventRepository eventRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private DiscountCodeRepository discountRepository;

    private Cookie cookie;
    private String boxOfSix;

    @BeforeEach
    void setUp() {
        cookie = cookieRepository.save(new Cookie.Builder().setDescription("Double Chocolate Chunk").setSlug("chunk")
                .setCategory(CookieCategory.LOW_SUGAR).setPrice(30.0).setStockQuantity(12)
                .setIngredients("Cocoa").setAllergies("None").build());
        boxOfSix = variantRepository.save(new CookieVariant.Builder().setCookieId(cookie.getCookieId())
                .setLabel("Box of 6").setUnitsPerPack(6).setPrice(new BigDecimal("170")).build()).getVariantId();
    }

    private record Placed(String orderNumber, String token, String cartId) {
    }

    private Placed place(ShippingMethod method) {
        String cartId = cartService.create(null).cartId();
        cartService.addItem(cartId, new AddItemRequest(boxOfSix, 1));
        var response = checkoutService.checkout(new CheckoutRequest(cartId, "thandi@example.co.za", "Thandi", "Mokoena",
                "0821234567", method, null, method == ShippingMethod.PUDO ? "PUDO Strandfontein Spar" : null,
                null, null), null);
        return new Placed(response.orderNumber(), response.accessToken(), cartId);
    }

    private CustomerOrder order(Placed placed) {
        return orderRepository.findByOrderNumber(placed.orderNumber()).orElseThrow();
    }

    private int stock() {
        return cookieRepository.findById(cookie.getCookieId()).orElseThrow().getStockQuantity();
    }

    private List<OrderStatus> timeline(Placed placed) {
        return eventRepository.findByOrderIdOrderByCreatedAtAsc(order(placed).getOrderId()).stream()
                .map(OrderStatusEvent::getStatus).toList();
    }

    @Test
    void theOrderPageNeedsTheRightToken() {
        Placed placed = place(ShippingMethod.PICKUP);

        OrderView view = orderService.view(placed.orderNumber(), placed.token());

        assertEquals(OrderStatus.PENDING_PAYMENT, view.status());
        assertTrue(view.canPay());
        assertEquals("1 Test Street, Strandfontein\nBring your order number.", view.pickupDetails());
        assertThrows(NotFoundException.class, () -> orderService.view(placed.orderNumber(), "wrong"));
        assertThrows(NotFoundException.class, () -> orderService.view(placed.orderNumber(), null));
    }

    @Test
    void payingIsRecordedOnceEvenIfPayFastRepeatsItself() {
        discountRepository.save(new DiscountCode.Builder().setCode("TREAT").setType(DiscountType.FIXED)
                .setValue(BigDecimal.TEN).setMaxUses(5).build());
        String cartId = cartService.create(null).cartId();
        cartService.addItem(cartId, new AddItemRequest(boxOfSix, 1));
        cartService.applyDiscount(cartId, "TREAT");
        var response = checkoutService.checkout(new CheckoutRequest(cartId, "t@example.co.za", "Thandi", "Mokoena",
                null, ShippingMethod.PICKUP, null, null, null, null), null);

        orderService.markPaid(response.orderNumber(), "payfast", "Paid");
        orderService.markPaid(response.orderNumber(), "payfast", "Paid again");

        CustomerOrder order = orderRepository.findByOrderNumber(response.orderNumber()).orElseThrow();
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertNotNull(order.getPaidAt());
        assertEquals(1, discountRepository.findByCode("TREAT").orElseThrow().getUsedCount());
        assertEquals(6, stock());
        // A guest order only counts as a verified purchase once its owner claims it into an account.
        assertFalse(purchaseVerifier.hasBought("t@example.co.za", cookie.getCookieId()));
        orderRepository.save(new CustomerOrder.Builder().copy(order).setAccountOwned(true).build());
        assertTrue(purchaseVerifier.hasBought("t@example.co.za", cookie.getCookieId()));
        assertEquals(1L, siteInfoService.info().stats().customersServed());
        assertEquals(6L, siteInfoService.info().stats().cookiesSold());
    }

    @Test
    void cancellingAnUnpaidOrderFreesTheStockAndReopensTheCart() {
        Placed placed = place(ShippingMethod.PICKUP);
        assertEquals(6, stock());

        OrderView view = orderService.cancelUnpaid(placed.orderNumber(), placed.token());

        assertEquals(OrderStatus.CANCELLED, view.status());
        assertEquals(12, stock());
        assertEquals(CartStatus.ACTIVE, cartRepository.findById(placed.cartId()).orElseThrow().getStatus());
        assertThrows(ConflictException.class, () -> orderService.cancelUnpaid(placed.orderNumber(), placed.token()));
        assertThrows(ConflictException.class, () -> orderService.paymentRedirect(placed.orderNumber(), placed.token()));
    }

    @Test
    void ordersNotPaidInTimeAreCancelledAndTheirStockReturned() {
        Placed late = place(ShippingMethod.PICKUP);
        Placed onTime = place(ShippingMethod.PICKUP);
        orderRepository.save(new CustomerOrder.Builder().copy(order(late)).setReservedUntil(LocalDateTime.now().minusMinutes(1)).build());

        assertEquals(1, orderService.releaseExpired());

        assertEquals(OrderStatus.CANCELLED, order(late).getStatus());
        assertEquals(OrderStatus.PENDING_PAYMENT, order(onTime).getStatus());
        assertEquals(6, stock());
        assertEquals(CartStatus.ACTIVE, cartRepository.findById(late.cartId()).orElseThrow().getStatus());
    }

    @Test
    void aPaymentArrivingAfterExpiryStillCountsAndTakesTheStockBack() {
        Placed placed = place(ShippingMethod.PICKUP);
        orderRepository.save(new CustomerOrder.Builder().copy(order(placed)).setReservedUntil(LocalDateTime.now().minusMinutes(1)).build());
        orderService.releaseExpired();

        orderService.markPaid(placed.orderNumber(), "payfast", "Paid late");

        assertEquals(OrderStatus.PAID, order(placed).getStatus());
        assertEquals(6, stock());
        assertEquals(CartStatus.CHECKED_OUT, cartRepository.findById(placed.cartId()).orElseThrow().getStatus());
    }

    @Test
    void aFailedPaymentFreesTheStock() {
        Placed placed = place(ShippingMethod.PICKUP);

        orderService.markPaymentFailed(placed.orderNumber(), "payfast", "Declined");

        assertEquals(OrderStatus.PAYMENT_FAILED, order(placed).getStatus());
        assertEquals(12, stock());
    }

    @Test
    void aPickupOrderGoesFromPaidToCollected() {
        Placed placed = place(ShippingMethod.PICKUP);
        orderService.markPaid(placed.orderNumber(), "payfast", "Paid");

        adminOrderService.changeStatus(placed.orderNumber(), new StatusChangeRequest(OrderStatus.PROCESSING, "Baking"), "AD001");
        adminOrderService.changeStatus(placed.orderNumber(), new StatusChangeRequest(OrderStatus.READY_FOR_PICKUP, null), "AD001");
        AdminOrderView done = adminOrderService.changeStatus(placed.orderNumber(), new StatusChangeRequest(OrderStatus.DELIVERED, "Collected"), "AD001");

        assertEquals(OrderStatus.DELIVERED, done.order().status());
        assertEquals(List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.PROCESSING,
                OrderStatus.READY_FOR_PICKUP, OrderStatus.DELIVERED), timeline(placed));
        assertEquals("AD001", done.events().getLast().getActor());
    }

    @Test
    void statusesCantSkipAhead() {
        Placed placed = place(ShippingMethod.PUDO);

        assertThrows(ConflictException.class, () -> adminOrderService.changeStatus(placed.orderNumber(),
                new StatusChangeRequest(OrderStatus.DELIVERED, null), "AD001"));
        orderService.markPaid(placed.orderNumber(), "payfast", "Paid");
        assertThrows(ConflictException.class, () -> adminOrderService.changeStatus(placed.orderNumber(),
                new StatusChangeRequest(OrderStatus.CANCELLED, null), "AD001"));
        adminOrderService.changeStatus(placed.orderNumber(), new StatusChangeRequest(OrderStatus.PROCESSING, null), "AD001");
        assertThrows(ConflictException.class, () -> adminOrderService.changeStatus(placed.orderNumber(),
                new StatusChangeRequest(OrderStatus.READY_FOR_PICKUP, null), "AD001"));
    }

    @Test
    void shippingRecordsTrackingAndCanBeCorrected() {
        Placed placed = place(ShippingMethod.PUDO);
        orderService.markPaid(placed.orderNumber(), "payfast", "Paid");

        assertThrows(BadRequestException.class, () -> adminOrderService.ship(placed.orderNumber(),
                new TrackingRequest("PUDO", "TRK1", "http://not-secure.example"), "AD001"));
        adminOrderService.ship(placed.orderNumber(), new TrackingRequest("PUDO", "TRK1", null), "AD001");
        AdminOrderView corrected = adminOrderService.ship(placed.orderNumber(),
                new TrackingRequest("PUDO", "TRK2", "https://track.example/TRK2"), "AD001");

        assertEquals(OrderStatus.SHIPPED, corrected.order().status());
        assertEquals("TRK2", corrected.order().trackingNumber());
        assertTrue(corrected.events().getLast().getNote().startsWith("Tracking updated"));
    }

    @Test
    void pickupOrdersCantBeShipped() {
        Placed placed = place(ShippingMethod.PICKUP);
        orderService.markPaid(placed.orderNumber(), "payfast", "Paid");

        assertThrows(ConflictException.class, () -> adminOrderService.ship(placed.orderNumber(),
                new TrackingRequest("Courier", "T1", null), "AD001"));
    }

    @Test
    void aPaidOrderCanBeCancelledUntilTheBakeryAcceptsIt() {
        Placed cancelled = place(ShippingMethod.PUDO);
        Placed accepted = place(ShippingMethod.PUDO);
        assertThrows(ConflictException.class, () -> orderService.cancelPaid(cancelled.orderNumber(), cancelled.token()));

        orderService.markPaid(cancelled.orderNumber(), "payfast", "Paid");
        orderService.markPaid(accepted.orderNumber(), "payfast", "Paid");
        adminOrderService.changeStatus(accepted.orderNumber(), new StatusChangeRequest(OrderStatus.PROCESSING, null), "AD001");
        int before = stock();

        OrderView view = orderService.cancelPaid(cancelled.orderNumber(), cancelled.token());

        assertEquals(OrderStatus.REFUNDED, view.status());
        assertEquals(before + 6, stock());
        assertEquals(List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.REFUNDED), timeline(cancelled));
        assertThrows(ConflictException.class, () -> orderService.cancelPaid(cancelled.orderNumber(), cancelled.token()));
        assertThrows(ConflictException.class, () -> orderService.cancelPaid(accepted.orderNumber(), accepted.token()));
        assertThrows(NotFoundException.class, () -> orderService.cancelPaid(accepted.orderNumber(), "wrong"));
        assertEquals(OrderStatus.PROCESSING, order(accepted).getStatus());
    }

    @Test
    void theInvoiceAndReceiptArePdfsOnceTheOrderIsPaid() throws IOException {
        Placed placed = place(ShippingMethod.PUDO);
        assertThrows(ConflictException.class,
                () -> orderService.document(placed.orderNumber(), placed.token(), OrderDocuments.Kind.INVOICE));
        assertThrows(NotFoundException.class,
                () -> orderService.document(placed.orderNumber(), "wrong", OrderDocuments.Kind.RECEIPT));

        orderService.markPaid(placed.orderNumber(), "payfast", "Paid");

        for (OrderDocuments.Kind kind : OrderDocuments.Kind.values()) {
            OrderService.Document document = orderService.document(placed.orderNumber(), placed.token(), kind);
            assertTrue(document.fileName().endsWith("-" + placed.orderNumber() + ".pdf"));
            try (PDDocument pdf = Loader.loadPDF(document.pdf())) {
                String text = new PDFTextStripper().getText(pdf);
                assertTrue(text.contains(placed.orderNumber()));
                assertTrue(text.contains("Thandi Mokoena"));
                assertTrue(text.toUpperCase().contains("PAID"));
            }
        }
    }

    @Test
    void refundingBeforeShippingRestocksButAfterShippingDoesNot() {
        Placed early = place(ShippingMethod.PUDO);
        Placed late = place(ShippingMethod.PUDO);
        orderService.markPaid(early.orderNumber(), "payfast", "Paid");
        orderService.markPaid(late.orderNumber(), "payfast", "Paid");
        adminOrderService.ship(late.orderNumber(), new TrackingRequest("PUDO", "T1", null), "AD001");
        assertEquals(0, stock());

        adminOrderService.refund(early.orderNumber(), "Changed their mind", "AD001");
        adminOrderService.refund(late.orderNumber(), null, "AD001");

        assertEquals(6, stock());
        assertEquals(OrderStatus.REFUNDED, order(early).getStatus());
        assertFalse(purchaseVerifier.hasBought("thandi@example.co.za", cookie.getCookieId()));
        assertThrows(ConflictException.class, () -> adminOrderService.refund(early.orderNumber(), null, "AD001"));
    }

    @Test
    void adminsCanFindOrdersByNumberEmailOrName() {
        Placed placed = place(ShippingMethod.PICKUP);

        assertEquals(1, adminOrderService.search(placed.orderNumber().toLowerCase(), null, null, null, null, null).totalItems());
        assertEquals(1, adminOrderService.search("mokoena", OrderStatus.PENDING_PAYMENT, null, null, null, null).totalItems());
        assertEquals(0, adminOrderService.search("mokoena", OrderStatus.PAID, null, null, null, null).totalItems());
        assertEquals(1, adminOrderService.search(null, null, null, null, null, null).items().getFirst().itemCount());
    }

    @Test
    void aSingleUseCodeCantSitOnTwoUnpaidOrdersAndComesBackWhenOneIsCancelled() {
        discountRepository.save(new DiscountCode.Builder().setCode("ONCE").setType(DiscountType.FIXED)
                .setValue(BigDecimal.TEN).setMaxUses(1).build());
        Placed first = placeWithCode("ONCE", "first@example.co.za");

        // The cart already refuses it: the unpaid order has taken the only use.
        assertThrows(BadRequestException.class, () -> placeWithCode("ONCE", "second@example.co.za"));
        assertEquals(1, discountRepository.findByCode("ONCE").orElseThrow().getUsedCount());

        orderService.cancelUnpaid(first.orderNumber(), first.token());
        assertEquals(0, discountRepository.findByCode("ONCE").orElseThrow().getUsedCount());
        placeWithCode("ONCE", "second@example.co.za");
    }

    @Test
    void oncePerCustomerCountsUnpaidOrdersToo() {
        discountRepository.save(new DiscountCode.Builder().setCode("HELLO").setType(DiscountType.FIXED)
                .setValue(BigDecimal.TEN).setOnePerEmail(true).build());
        placeWithCode("HELLO", "same@example.co.za");

        assertThrows(ConflictException.class, () -> placeWithCode("HELLO", "same@example.co.za"));
    }

    @Test
    void anExpiredHoldGivesTheCodeUseBack() {
        discountRepository.save(new DiscountCode.Builder().setCode("LATE").setType(DiscountType.FIXED)
                .setValue(BigDecimal.TEN).setMaxUses(1).build());
        Placed placed = placeWithCode("LATE", "late@example.co.za");
        orderRepository.save(new CustomerOrder.Builder().copy(order(placed))
                .setReservedUntil(LocalDateTime.now().minusMinutes(1)).build());

        orderService.releaseExpired();
        assertEquals(0, discountRepository.findByCode("LATE").orElseThrow().getUsedCount());

        orderService.markPaid(placed.orderNumber(), "payfast", "Paid late");
        assertEquals(1, discountRepository.findByCode("LATE").orElseThrow().getUsedCount());
    }

    @Test
    void refundingALatePaymentThatNeverGotItsCookiesDoesntInventStock() {
        Placed placed = place(ShippingMethod.PICKUP);
        orderRepository.save(new CustomerOrder.Builder().copy(order(placed))
                .setReservedUntil(LocalDateTime.now().minusMinutes(1)).build());
        orderService.releaseExpired();
        cookieRepository.save(new Cookie.Builder().copy(cookieRepository.findById(cookie.getCookieId()).orElseThrow())
                .setStockQuantity(0).build());

        orderService.markPaid(placed.orderNumber(), "payfast", "Paid late");
        adminOrderService.refund(placed.orderNumber(), "Couldn't bake it", "AD001");

        assertEquals(0, stock());
    }

    @Test
    void anAdminCancellingAnUnpaidOrderGivesTheShopperTheirCartBack() {
        Placed placed = place(ShippingMethod.PICKUP);

        adminOrderService.changeStatus(placed.orderNumber(), new StatusChangeRequest(OrderStatus.CANCELLED, null), "AD001");

        assertEquals(CartStatus.ACTIVE, cartRepository.findById(placed.cartId()).orElseThrow().getStatus());
        assertEquals(12, stock());
    }

    @Test
    void oneEmailCantHoldMoreThanThreeUnpaidOrders() {
        for (int i = 0; i < CheckoutService.MAX_UNPAID_ORDERS; i++) {
            place(ShippingMethod.PICKUP);
            cookieRepository.save(new Cookie.Builder().copy(cookieRepository.findById(cookie.getCookieId()).orElseThrow())
                    .setStockQuantity(12).build());
        }

        assertThrows(ConflictException.class, () -> place(ShippingMethod.PICKUP));
    }

    private Placed placeWithCode(String code, String email) {
        String cartId = cartService.create(null).cartId();
        cartService.addItem(cartId, new AddItemRequest(boxOfSix, 1));
        cartService.applyDiscount(cartId, code);
        var response = checkoutService.checkout(new CheckoutRequest(cartId, email, "Thandi", "Mokoena",
                null, ShippingMethod.PICKUP, null, null, null, null), null);
        return new Placed(response.orderNumber(), response.accessToken(), cartId);
    }
}
