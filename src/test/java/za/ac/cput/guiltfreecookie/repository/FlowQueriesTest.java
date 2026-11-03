package za.ac.cput.guiltfreecookie.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import za.ac.cput.guiltfreecookie.domain.Cart;
import za.ac.cput.guiltfreecookie.domain.CartItem;
import za.ac.cput.guiltfreecookie.domain.CartStatus;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.NewsletterSubscriber;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.domain.SubscriberStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The queries the email flows run to find who to email.
@DataJpaTest
class FlowQueriesTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private CustomerOrderRepository orderRepository;
    @Autowired
    private NewsletterSubscriberRepository subscriberRepository;

    @Test
    void idleCartsNeedAnEmailAndTheRightAge() {
        cart("idle", "a@example.com", CartStatus.ACTIVE, NOW.minusHours(2));
        cart("fresh", "b@example.com", CartStatus.ACTIVE, NOW.minusMinutes(10));
        cart("anonymous", null, CartStatus.ACTIVE, NOW.minusHours(2));
        cart("ordered", "c@example.com", CartStatus.CHECKED_OUT, NOW.minusHours(2));
        cart("ancient", "d@example.com", CartStatus.ACTIVE, NOW.minusDays(30));

        List<Cart> idle = cartRepository.findIdleWithEmail(NOW.minusHours(1), NOW.minusDays(7));

        assertEquals(List.of("idle"), idle.stream().map(Cart::getCartId).toList());
    }

    @Test
    void cartActivityIsMatchedByEmailAndTime() {
        cart("mine", "a@example.com", CartStatus.ACTIVE, NOW);
        cartItemRepository.save(new CartItem.Builder().setCartId("mine").setVariantId("v1").setCookieId("c1")
                .setQuantity(1).setAddedAt(NOW).build());

        assertTrue(cartItemRepository.addedByEmailSince("a@example.com", NOW.minusMinutes(5)));
        assertFalse(cartItemRepository.addedByEmailSince("a@example.com", NOW.plusMinutes(5)));
        assertFalse(cartItemRepository.addedByEmailSince("someone@example.com", NOW.minusMinutes(5)));
    }

    @Test
    void lapsedCustomersAreFoundByTheirLastPaidOrder() {
        order("GFC-1", "lapsed@example.com", OrderStatus.DELIVERED, NOW.minusDays(70));
        order("GFC-2", "recent@example.com", OrderStatus.DELIVERED, NOW.minusDays(70));
        order("GFC-3", "recent@example.com", OrderStatus.PAID, NOW.minusDays(5));
        order("GFC-4", "unpaid@example.com", OrderStatus.CANCELLED, NOW.minusDays(70));

        List<Object[]> lapsed = orderRepository.lastPaidBetween(OrderStatus.PAID_STATUSES, NOW.minusDays(90), NOW.minusDays(60));

        assertEquals(List.of("lapsed@example.com"), lapsed.stream().map(row -> (String) row[0]).toList());
        assertTrue(orderRepository.existsByCustomerEmailAndStatusInAndPaidAtAfter(
                "recent@example.com", OrderStatus.PAID_STATUSES, NOW.minusDays(6)));
    }

    @Test
    void disengagedSubscribersExcludeTheNewTheActiveAndTheUnsubscribed() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(90));
        subscriber("quiet@example.com", cutoff.minus(Duration.ofDays(10)), null, null);
        subscriber("new@example.com", Instant.now(), null, null);
        subscriber("clicker@example.com", cutoff.minus(Duration.ofDays(10)), Instant.now(), null);
        subscriber("gone@example.com", cutoff.minus(Duration.ofDays(10)), null, SubscriberStatus.UNSUBSCRIBED);

        assertEquals(List.of("quiet@example.com"),
                subscriberRepository.findDisengaged(cutoff).stream().map(NewsletterSubscriber::getEmail).toList());
    }

    private void cart(String id, String email, CartStatus status, LocalDateTime updatedAt) {
        cartRepository.save(new Cart.Builder().setCartId(id).setCustomerEmail(email).setStatus(status)
                .setCreatedAt(updatedAt).setUpdatedAt(updatedAt).build());
    }

    private void order(String number, String email, OrderStatus status, LocalDateTime paidAt) {
        orderRepository.save(new CustomerOrder.Builder().setOrderNumber(number).setCustomerEmail(email)
                .setFirstName("Thandi").setLastName("Mokoena").setShippingMethod(ShippingMethod.PICKUP)
                .setSubtotal(BigDecimal.TEN).setDiscountTotal(BigDecimal.ZERO).setShippingTotal(BigDecimal.ZERO)
                .setTotal(BigDecimal.TEN).setStatus(status).setPaidAt(status.isPaid() ? paidAt : null)
                .setCreatedAt(paidAt).build());
    }

    private void subscriber(String email, Instant subscribedAt, Instant engagedAt, SubscriberStatus status) {
        subscriberRepository.save(new NewsletterSubscriber.Builder().setEmail(email).setSubscribedAt(subscribedAt)
                .setLastEngagedAt(engagedAt).setStatus(status).build());
    }
}
