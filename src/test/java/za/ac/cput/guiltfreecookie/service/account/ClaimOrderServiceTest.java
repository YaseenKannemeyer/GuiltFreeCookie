package za.ac.cput.guiltfreecookie.service.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import za.ac.cput.guiltfreecookie.domain.*;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.repository.account.*;
import za.ac.cput.guiltfreecookie.service.notification.EmailService;

class ClaimOrderServiceTest {

    String owner = "sam@example.com";
    Instant now = Instant.parse("2026-10-25T08:00:00Z");
    ClaimOrderLinks links = new ClaimOrderLinks(
        "test-secret",
        "https://shop.example",
        Clock.fixed(now, ZoneOffset.UTC)
    );
    AccountOrderRepository orders = mock(AccountOrderRepository.class);
    AccountCustomerRepository customers = mock(AccountCustomerRepository.class);
    EmailService emails = mock(EmailService.class);
    ClaimOrderService service = new ClaimOrderService(orders, customers, links, emails);

    @Test
    void validTokenIsBoundToInboxAndExpiresAt24Hours() {
        String token = links.token(owner);
        assertDoesNotThrow(() -> links.verify(token, owner));
        assertThrows(BadRequestException.class, () -> links.verify(token, "other@example.com"));
        assertThrows(BadRequestException.class, () ->
            links.verify(token.substring(0, token.length() - 5) + "abcde", owner)
        );
        var later = new ClaimOrderLinks(
            "test-secret",
            "https://shop.example",
            Clock.fixed(now.plusSeconds(86400), ZoneOffset.UTC)
        );
        assertThrows(BadRequestException.class, () -> later.verify(token, owner));
        for (String bad : Arrays.asList(null, "", "invalid", "a.b.c", "x".repeat(1025)))
            assertThrows(BadRequestException.class, () -> links.verify(bad, owner));
        assertThrows(BadRequestException.class, () ->
            new ClaimOrderLinks("other-key", "https://shop.example").verify(token, owner)
        );
    }

    @Test
    void onlyAccountInboxGetsLinkWithoutOrderDetails() {
        when(orders.countClaimable(owner)).thenReturn(2L);
        service.request(owner);
        verify(emails).sendTemplate(
            eq(owner),
            anyString(),
            eq("claim-orders"),
            argThat(
                m ->
                    m.size() == 2 &&
                    m.get("actionUrl").toString().startsWith("https://shop.example/account/claim?token=")
            ),
            eq(false)
        );
    }

    @Test
    void zeroClaimableOrdersDoesNotSend() {
        service.request(owner);
        verifyNoInteractions(emails);
    }

    @Test
    void invalidTokenNeverReadsOrChangesOrders() {
        assertThrows(BadRequestException.class, () -> service.confirm(owner, "bad"));
        verifyNoInteractions(orders, customers);
    }

    @Test
    void confirmsWithBuilderAndRepeatedClaimIsHarmless() {
        when(customers.lock(owner)).thenReturn(Optional.of(new Customer.Builder().setCustomerEmail(owner).build()));
        var order = new CustomerOrder.Builder()
            .setOrderId("o1")
            .setOrderNumber("GFC-1")
            .setCustomerEmail(owner)
            .setStatus(OrderStatus.PAID)
            .setPaidAt(LocalDateTime.now())
            .build();
        when(orders.lockClaimable(owner)).thenReturn(List.of(order), List.of());
        assertEquals(1, service.confirm(owner, links.token(owner)));
        assertEquals(0, service.confirm(owner, links.token(owner)));
        verify(orders, times(1)).save(
            argThat(
                o -> o.isAccountOwned() && o.getOrderNumber().equals("GFC-1") && o.getPaidAt().equals(order.getPaidAt())
            )
        );
    }
}
