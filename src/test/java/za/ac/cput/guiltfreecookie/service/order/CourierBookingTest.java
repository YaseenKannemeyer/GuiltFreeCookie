package za.ac.cput.guiltfreecookie.service.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.Province;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.dto.cart.AddItemRequest;
import za.ac.cput.guiltfreecookie.dto.order.AddressRequest;
import za.ac.cput.guiltfreecookie.dto.order.CheckoutRequest;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CookieVariantRepository;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;
import za.ac.cput.guiltfreecookie.service.cart.ShopCartService;
import za.ac.cput.guiltfreecookie.service.shipping.ShipLogicClient;
import za.ac.cput.guiltfreecookie.service.shipping.ShipLogicException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "app.shiplogic.booking-days=MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY")
@Transactional
class CourierBookingTest {

    @MockitoBean
    private ShipLogicClient shipLogic;
    @Autowired
    private CourierBookingService courierBookingService;
    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private AdminOrderService adminOrderService;
    @Autowired
    private ShopCartService cartService;
    @Autowired
    private CookieRepository cookieRepository;
    @Autowired
    private CookieVariantRepository variantRepository;
    @Autowired
    private CustomerOrderRepository orderRepository;
    @Autowired
    private OrderItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        when(shipLogic.enabled()).thenReturn(true);
        when(shipLogic.trackingUrl("S8WW8L")).thenReturn("https://track.example/?ref=S8WW8L");
    }

    private String paidOrder(ShippingMethod method) {
        Cookie cookie = cookieRepository.save(new Cookie.Builder().setDescription("Oat Crunch").setSlug("oat-crunch-" + method.name().toLowerCase() + "-" + System.nanoTime())
                .setCategory(CookieCategory.LOW_SUGAR).setPrice(5.0).setStockQuantity(200)
                .setIngredients("Oats").setAllergies("Gluten").build());
        String box = variantRepository.save(new CookieVariant.Builder().setCookieId(cookie.getCookieId()).setLabel("Box of 24")
                .setUnitsPerPack(24).setPrice(new BigDecimal("84.00")).build()).getVariantId();
        String cartId = cartService.create(null).cartId();
        cartService.addItem(cartId, new AddItemRequest(box, 2));
        AddressRequest address = method == ShippingMethod.COURIER
                ? new AddressRequest("12", "Main Road", "Unit 4", "Rondebosch", "Cape Town", Province.WESTERN_CAPE, "7700") : null;
        String number = checkoutService.checkout(new CheckoutRequest(cartId, "sam@example.com", "Sam", "Baker", "0821234567",
                method, address, null, "Ring the bell", null), null).orderNumber();
        orderService.markPaid(number, "payfast", "Paid");
        return number;
    }

    private CustomerOrder order(String number) {
        return orderRepository.findByOrderNumber(number).orElseThrow();
    }

    @Test
    void bookingShipsTheOrderWithTheCourierTracking() {
        String number = paidOrder(ShippingMethod.COURIER);
        when(shipLogic.book(eq(number), any(), eq(48), any())).thenReturn(new ShipLogicClient.Booking("S8WW8L", new BigDecimal("95")));

        assertTrue(adminOrderService.get(number).courierBooking());
        courierBookingService.book(number, "admin-1");

        CustomerOrder shipped = order(number);
        assertEquals(OrderStatus.SHIPPED, shipped.getStatus());
        assertEquals("The Courier Guy", shipped.getCourierName());
        assertEquals("S8WW8L", shipped.getTrackingNumber());
        assertEquals("https://track.example/?ref=S8WW8L", shipped.getTrackingUrl());
        verify(shipLogic).book(eq(number), any(), eq(48), eq(new BigDecimal("168.00")));
        assertThrows(ConflictException.class, () -> courierBookingService.book(number, "admin-1"));
    }

    @Test
    void aRefusedBookingLeavesTheOrderPaidAndSaysWhy() {
        String number = paidOrder(ShippingMethod.COURIER);
        when(shipLogic.book(any(), any(), anyInt(), any())).thenThrow(new ShipLogicException("The Courier Guy said: invalid address"));

        ConflictException error = assertThrows(ConflictException.class, () -> courierBookingService.book(number, "admin-1"));

        assertEquals("The Courier Guy said: invalid address", error.getMessage());
        assertEquals(OrderStatus.PAID, order(number).getStatus());
    }

    @Test
    void onlyPaidCourierOrdersOnBookingDaysAreBooked() {
        String pickup = paidOrder(ShippingMethod.PICKUP);
        assertThrows(ConflictException.class, () -> courierBookingService.book(pickup, "admin-1"));

        String courier = paidOrder(ShippingMethod.COURIER);
        Clock friday = Clock.fixed(Instant.parse("2026-11-06T08:00:00Z"), ZoneId.of("Africa/Johannesburg"));
        CourierBookingService mondayToWednesday = new CourierBookingService(orderService, adminOrderService, orderRepository,
                itemRepository, shipLogic, EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY), friday);
        ConflictException error = assertThrows(ConflictException.class, () -> mondayToWednesday.book(courier, "admin-1"));
        assertTrue(error.getMessage().startsWith("Couriers are only booked on Monday, Tuesday, Wednesday"));
        verify(shipLogic, never()).book(any(), any(), anyInt(), any());
    }

    @Test
    void deliveredParcelsAreMarkedDelivered() {
        String number = paidOrder(ShippingMethod.COURIER);
        when(shipLogic.book(any(), any(), anyInt(), any())).thenReturn(new ShipLogicClient.Booking("S8WW8L", new BigDecimal("95")));
        courierBookingService.book(number, "admin-1");

        when(shipLogic.status("S8WW8L")).thenReturn(Optional.of("in-transit"));
        courierBookingService.checkDeliveries();
        assertEquals(OrderStatus.SHIPPED, order(number).getStatus());

        when(shipLogic.status("S8WW8L")).thenReturn(Optional.of("delivered"));
        courierBookingService.checkDeliveries();
        assertEquals(OrderStatus.DELIVERED, order(number).getStatus());
    }

    @Test
    void theWaybillComesFromTheCourier() {
        String number = paidOrder(ShippingMethod.COURIER);
        assertThrows(ConflictException.class, () -> courierBookingService.waybillUrl(number));
        when(shipLogic.book(any(), any(), anyInt(), any())).thenReturn(new ShipLogicClient.Booking("S8WW8L", new BigDecimal("95")));
        courierBookingService.book(number, "admin-1");
        when(shipLogic.waybillUrl("S8WW8L")).thenReturn("https://labels.example/S8WW8L.pdf");

        assertEquals("https://labels.example/S8WW8L.pdf", courierBookingService.waybillUrl(number));
    }
}
