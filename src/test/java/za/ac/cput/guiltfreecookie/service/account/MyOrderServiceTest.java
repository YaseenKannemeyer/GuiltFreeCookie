package za.ac.cput.guiltfreecookie.service.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import za.ac.cput.guiltfreecookie.domain.*;
import za.ac.cput.guiltfreecookie.dto.order.OrderView;
import za.ac.cput.guiltfreecookie.exception.*;
import za.ac.cput.guiltfreecookie.repository.*;
import za.ac.cput.guiltfreecookie.repository.account.AccountOrderRepository;
import za.ac.cput.guiltfreecookie.service.order.*;

class MyOrderServiceTest {

    AccountOrderRepository orders = mock(AccountOrderRepository.class);
    OrderItemRepository items = mock(OrderItemRepository.class);
    CookieRepository cookies = mock(CookieRepository.class);
    OrderViews views = mock(OrderViews.class);
    OrderLinks links = new OrderLinks("test-secret", "https://shop.example");
    MyOrderService service = new MyOrderService(orders, items, cookies, links, views);
    CustomerOrder order = new CustomerOrder.Builder()
        .setOrderId("o1")
        .setOrderNumber("GFC-1")
        .setCustomerEmail("sam@example.com")
        .setAccountOwned(true)
        .setStatus(OrderStatus.PAID)
        .setTotal(new BigDecimal("80.00"))
        .build();

    @Test
    void summariesUseOwnedQueryBatchImagesAndTrackingTokens() {
        when(orders.history(eq("sam@example.com"), any(), any())).thenReturn(new PageImpl<>(List.of(order)));
        when(items.findByOrderIdIn(List.of("o1"))).thenReturn(
            List.of(
                new OrderItem.Builder().setOrderItemId("i1").setOrderId("o1").setCookieId("c1").setQuantity(2).build()
            )
        );
        when(cookies.findAllById(List.of("c1"))).thenReturn(
            List.of(new Cookie.Builder().setCookieId("c1").setImage("cookies/photo.webp").build())
        );
        var result = service.history("sam@example.com", 0, 10);
        var row = result.items().getFirst();
        assertEquals(2, row.itemCount());
        assertEquals("cookies/photo.webp", row.image());
        assertTrue(links.matches(order, row.token()));
        assertEquals(new BigDecimal("80.00"), row.total());
        verify(items, times(1)).findByOrderIdIn(any());
        verify(cookies, times(1)).findAllById(any());
    }

    @Test
    void missingOrOtherOwnersOrderIsAlways404() {
        assertThrows(NotFoundException.class, () -> service.get("other@example.com", "GFC-1"));
        verify(orders).findByOrderNumberAndCustomerEmailAndAccountOwnedTrue("GFC-1", "other@example.com");
        verifyNoInteractions(views);
    }

    @Test
    void detailIncludesExistingViewAndToken() {
        when(orders.findByOrderNumberAndCustomerEmailAndAccountOwnedTrue("GFC-1", "sam@example.com")).thenReturn(
            Optional.of(order)
        );
        var view = mock(OrderView.class);
        when(views.view(order)).thenReturn(view);
        var result = service.get("sam@example.com", "GFC-1");
        assertSame(view, result.order());
        assertTrue(links.matches(order, result.token()));
    }

    @Test
    void validatesPaginationAndDoesNotFetchLinesForEmptyPages() {
        assertThrows(BadRequestException.class, () -> service.history("sam", -1, 10));
        assertThrows(BadRequestException.class, () -> service.history("sam", 0, 101));
        assertThrows(BadRequestException.class, () -> service.history("sam", 0, 0));
        when(orders.history(eq("sam"), any(), any())).thenReturn(Page.empty());
        assertTrue(service.history("sam", null, null).items().isEmpty());
        verifyNoInteractions(items, cookies);
    }
}
