package za.ac.cput.guiltfreecookie.repository.account;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.*;
import za.ac.cput.guiltfreecookie.domain.*;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;

@DataJpaTest
class AccountOrderRepositoryTest {

    @Autowired
    AccountOrderRepository accountOrders;

    @Autowired
    CustomerOrderRepository orders;

    LocalDateTime now = LocalDateTime.of(2026, 10, 25, 8, 0);

    CustomerOrder save(
        String number,
        String email,
        boolean owned,
        OrderStatus status,
        boolean paid,
        LocalDateTime reserved,
        int age
    ) {
        return orders.saveAndFlush(
            new CustomerOrder.Builder()
                .setOrderNumber(number)
                .setCustomerEmail(email)
                .setAccountOwned(owned)
                .setFirstName("Sam")
                .setLastName("Baker")
                .setShippingMethod(ShippingMethod.PICKUP)
                .setStatus(status)
                .setCreatedAt(now.minusHours(age))
                .setReservedUntil(reserved)
                .setPaidAt(paid ? now.minusHours(age) : null)
                .setSubtotal(BigDecimal.TEN)
                .setDiscountTotal(BigDecimal.ZERO)
                .setShippingTotal(BigDecimal.ZERO)
                .setTotal(BigDecimal.TEN)
                .build()
        );
    }

    @Test
    void historyFiltersOwnershipAndAbandonedAttemptsBeforePagination() {
        save("paid", "me@example.com", true, OrderStatus.PAID, true, null, 1);
        save("active", "me@example.com", true, OrderStatus.PENDING_PAYMENT, false, now.plusMinutes(10), 0);
        save("expired", "me@example.com", true, OrderStatus.PENDING_PAYMENT, false, now.minusSeconds(1), 2);
        save("boundary", "me@example.com", true, OrderStatus.PENDING_PAYMENT, false, now, 3);
        save("cancelled", "me@example.com", true, OrderStatus.CANCELLED, false, null, 4);
        save("paid-cancel", "me@example.com", true, OrderStatus.CANCELLED, true, null, 5);
        save("refunded", "me@example.com", true, OrderStatus.REFUNDED, true, null, 6);
        save("guest", "me@example.com", false, OrderStatus.PAID, true, null, 7);
        save("other", "other@example.com", true, OrderStatus.PAID, true, null, 8);
        var first = accountOrders.history(
            "me@example.com",
            now,
            PageRequest.of(0, 2, Sort.by("createdAt").descending())
        );
        assertEquals(4, first.getTotalElements());
        assertEquals(
            List.of("active", "paid"),
            first.getContent().stream().map(CustomerOrder::getOrderNumber).toList()
        );
        var second = accountOrders.history(
            "me@example.com",
            now,
            PageRequest.of(1, 2, Sort.by("createdAt").descending())
        );
        assertEquals(
            List.of("paid-cancel", "refunded"),
            second.getContent().stream().map(CustomerOrder::getOrderNumber).toList()
        );
        assertTrue(
            accountOrders.findByOrderNumberAndCustomerEmailAndAccountOwnedTrue("guest", "me@example.com").isEmpty()
        );
        assertTrue(
            accountOrders.findByOrderNumberAndCustomerEmailAndAccountOwnedTrue("other", "me@example.com").isEmpty()
        );
    }

    @Test
    void claimsOnlyPreviouslyPaidUnownedOrdersForTheSignedInInbox() {
        save("paid", "me@example.com", false, OrderStatus.PAID, true, null, 1);
        save("refund", "me@example.com", false, OrderStatus.REFUNDED, true, null, 2);
        save("unpaid", "me@example.com", false, OrderStatus.PENDING_PAYMENT, false, now.plusMinutes(20), 0);
        save("owned", "me@example.com", true, OrderStatus.PAID, true, null, 3);
        save("other", "other@example.com", false, OrderStatus.PAID, true, null, 4);
        assertEquals(2, accountOrders.countClaimable("me@example.com"));
        accountOrders
            .lockClaimable("me@example.com")
            .forEach(o -> accountOrders.save(new CustomerOrder.Builder().copy(o).setAccountOwned(true).build()));
        assertEquals(0, accountOrders.countClaimable("me@example.com"));
        assertEquals(1, accountOrders.countClaimable("other@example.com"));
    }
}
