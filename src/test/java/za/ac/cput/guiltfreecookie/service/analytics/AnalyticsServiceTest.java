package za.ac.cput.guiltfreecookie.service.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Cart;
import za.ac.cput.guiltfreecookie.domain.CartStatus;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.EmailFlow;
import za.ac.cput.guiltfreecookie.domain.EmailSend;
import za.ac.cput.guiltfreecookie.domain.OrderItem;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.dto.analytics.AnalyticsSummary;
import za.ac.cput.guiltfreecookie.dto.analytics.AnalyticsSummary.FlowResult;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.EmailSendRepository;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnalyticsServiceTest {

    // A fixed past week, so the test doesn't depend on today's date.
    private static final LocalDate FROM = LocalDate.of(2026, 3, 9);
    private static final LocalDate TO = LocalDate.of(2026, 3, 15);

    @Autowired
    private AnalyticsService analyticsService;
    @Autowired
    private CustomerOrderRepository orderRepository;
    @Autowired
    private OrderItemRepository itemRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private EmailSendRepository sendRepository;
    @Autowired
    private MockMvc mvc;

    @Test
    void summarisesPaidOrdersAgainstThePreviousPeriod() {
        order("GFC-A1", "regular@example.co.za", OrderStatus.PAID, noonOn(FROM.minusDays(3)), "100.00", "c1", 1, 6);
        order("GFC-A2", "regular@example.co.za", OrderStatus.DELIVERED, noonOn(FROM.plusDays(1)), "200.00", "c1", 2, 6);
        order("GFC-A3", "new@example.co.za", OrderStatus.SHIPPED, noonOn(FROM.plusDays(1)), "50.00", "c2", 1, 1);
        order("GFC-A4", "gone@example.co.za", OrderStatus.REFUNDED, noonOn(FROM.plusDays(2)), "80.00", "c2", 1, 1);
        order("GFC-A5", "late@example.co.za", OrderStatus.PENDING_PAYMENT, null, "70.00", "c2", 1, 1);

        AnalyticsSummary summary = analyticsService.summary(FROM, TO);

        assertEquals(new BigDecimal("250.00"), summary.current().revenue());
        assertEquals(2, summary.current().orders());
        assertEquals(new BigDecimal("125.00"), summary.current().averageOrderValue());
        assertEquals(13, summary.current().cookiesSold());
        assertEquals(1, summary.current().refunds());
        assertEquals(new BigDecimal("100.00"), summary.previous().revenue());
        assertEquals(7, summary.days().size());
        assertEquals(new BigDecimal("250.00"), summary.days().get(1).revenue());
        assertEquals("c1", summary.topCookies().getFirst().cookieId());
        assertEquals(12, summary.topCookies().getFirst().cookies());
        assertEquals(1, summary.customers().newCustomers());
        assertEquals(1, summary.customers().returningCustomers());
        assertEquals(4, summary.funnel().ordersPlaced());
        assertEquals(3, summary.funnel().ordersPaid());
    }

    @Test
    void creditsAnOrderToTheEmailClickedJustBeforeIt() {
        LocalDateTime paid = noonOn(FROM.plusDays(3));
        order("GFC-B1", "clicker@example.co.za", OrderStatus.PAID, paid, "120.00", "c1", 1, 6);
        order("GFC-B2", "slow@example.co.za", OrderStatus.PAID, paid, "90.00", "c1", 1, 6);
        click("clicker@example.co.za", EmailFlow.WELCOME, paid.minusDays(3));
        click("clicker@example.co.za", EmailFlow.CART_ABANDONMENT, paid.minusHours(2));
        click("slow@example.co.za", EmailFlow.WINBACK, paid.minusDays(9));

        AnalyticsSummary summary = analyticsService.summary(FROM, TO);

        assertEquals(new BigDecimal("120.00"), flow(summary, EmailFlow.CART_ABANDONMENT).revenue());
        assertEquals(1, flow(summary, EmailFlow.CART_ABANDONMENT).orders());
        assertEquals(0, flow(summary, EmailFlow.WELCOME).orders());
        assertEquals(0, flow(summary, EmailFlow.WINBACK).orders());
        assertEquals(7, summary.emailFlows().size());
    }

    @Test
    void countsTheCheckoutFunnel() {
        cart("full", "a@example.co.za", noonOn(FROM));
        cart("anonymous", null, noonOn(FROM));
        cart("earlier", "b@example.co.za", noonOn(FROM.minusDays(1)));

        AnalyticsSummary summary = analyticsService.summary(FROM, TO);

        assertEquals(2, summary.funnel().carts());
        assertEquals(1, summary.funnel().cartsWithEmail());
    }

    @Test
    void rejectsBackwardsOrHugePeriods() {
        assertThrows(BadRequestException.class, () -> analyticsService.summary(TO, FROM));
        assertThrows(BadRequestException.class, () -> analyticsService.summary(FROM.minusYears(2), TO));
    }

    @Test
    void theHealthCheckIsPublicAndSaysNothingElse() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/analytics")).andExpect(status().isUnauthorized());
    }

    private FlowResult flow(AnalyticsSummary summary, EmailFlow flow) {
        return summary.emailFlows().stream().filter(result -> result.flow() == flow).findFirst().orElseThrow();
    }

    private void order(String number, String email, OrderStatus status, LocalDateTime paidAt, String total,
                       String cookieId, int packs, int unitsPerPack) {
        BigDecimal amount = new BigDecimal(total);
        CustomerOrder order = orderRepository.save(new CustomerOrder.Builder().setOrderNumber(number).setCustomerEmail(email)
                .setFirstName("Thandi").setLastName("Mokoena").setShippingMethod(ShippingMethod.COURIER)
                .setSubtotal(amount).setDiscountTotal(BigDecimal.ZERO).setShippingTotal(BigDecimal.ZERO).setTotal(amount)
                .setStatus(status).setPaidAt(paidAt)
                .setCreatedAt(paidAt != null ? paidAt.minusMinutes(5) : noonOn(FROM.plusDays(4))).build());
        itemRepository.save(new OrderItem.Builder().setOrderId(order.getOrderId()).setCookieId(cookieId)
                .setVariantId("v-" + cookieId).setCookieName(cookieId.equals("c1") ? "Double Chocolate Chunk" : "Lemon Almond")
                .setVariantLabel("Box").setUnitsPerPack(unitsPerPack).setUnitPrice(amount).setQuantity(packs)
                .setLineTotal(amount).build());
    }

    private void click(String email, EmailFlow flow, LocalDateTime clickedAt) {
        sendRepository.save(new EmailSend.Builder().setSendId(UUID.randomUUID().toString().replace("-", ""))
                .setEmail(email).setFlow(flow).setStep(0).setSentAt(clickedAt.minusHours(1)).setClickedAt(clickedAt).build());
    }

    private void cart(String id, String email, LocalDateTime createdAt) {
        cartRepository.save(new Cart.Builder().setCartId(id).setCustomerEmail(email).setStatus(CartStatus.ACTIVE)
                .setCreatedAt(createdAt).setUpdatedAt(createdAt).build());
    }

    private static LocalDateTime noonOn(LocalDate day) {
        return AnalyticsService.startOf(day).plusHours(12);
    }
}
