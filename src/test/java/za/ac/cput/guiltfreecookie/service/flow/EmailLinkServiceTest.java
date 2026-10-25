package za.ac.cput.guiltfreecookie.service.flow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.guiltfreecookie.domain.Cart;
import za.ac.cput.guiltfreecookie.domain.CartStatus;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.OrderItem;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.dto.cart.CartView;
import za.ac.cput.guiltfreecookie.dto.flow.LinkedReviewRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;
import za.ac.cput.guiltfreecookie.service.cart.ShopCartService;
import za.ac.cput.guiltfreecookie.service.engagement.ReviewService;
import za.ac.cput.guiltfreecookie.service.order.OrderLinks;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailLinkServiceTest {

    private static final String EMAIL = "thandi@example.co.za";

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CustomerOrderRepository orderRepository;
    @Mock
    private OrderItemRepository itemRepository;
    @Mock
    private ShopCartService shopCartService;
    @Mock
    private ReviewService reviewService;
    @Mock
    private OrderLinks orderLinks;
    @Mock
    private MarketingLinks marketingLinks;
    @Mock
    private MarketingConsent consent;

    @InjectMocks
    private EmailLinkService service;

    @Test
    void accountCartsCantBeOpenedFromALink() {
        when(cartRepository.findByRecoveryToken("token")).thenReturn(Optional.of(new Cart.Builder()
                .setCartId("c1").setStatus(CartStatus.ACTIVE).setAccountOwned(true).build()));

        assertThrows(NotFoundException.class, () -> service.recoverCart("token"));
        verify(shopCartService, never()).get(anyString());
    }

    @Test
    void aReorderSkipsWhatsSoldOutAndFailsWhenNothingIsLeft() {
        CustomerOrder order = order(OrderStatus.DELIVERED);
        when(orderRepository.findByOrderNumber("GFC-1")).thenReturn(Optional.of(order));
        when(orderLinks.matches(order, "t")).thenReturn(true);
        when(itemRepository.findByOrderId("o1")).thenReturn(List.of(item("c1", "v1"), item("c2", "v2")));
        CartView cart = mock(CartView.class);
        when(cart.cartId()).thenReturn("new-cart");
        when(shopCartService.create(null)).thenReturn(cart);
        when(shopCartService.addItem(eq("new-cart"), any()))
                .thenThrow(new ConflictException("Sorry, Lemon Almond is sold out"))
                .thenReturn(cart);

        service.reorder("GFC-1", "t");
        verify(shopCartService).setEmail("new-cart", EMAIL);

        when(shopCartService.addItem(eq("new-cart"), any())).thenThrow(new ConflictException("sold out"));
        assertThrows(ConflictException.class, () -> service.reorder("GFC-1", "t"));
    }

    @Test
    void reviewsByLinkNeedADeliveredOrderContainingTheCookie() {
        CustomerOrder shipped = order(OrderStatus.SHIPPED);
        when(orderRepository.findByOrderNumber("GFC-1")).thenReturn(Optional.of(shipped));
        when(orderLinks.matches(any(), eq("t"))).thenReturn(true);
        LinkedReviewRequest request = new LinkedReviewRequest("GFC-1", "t", "c9", 5, "Yum", "Lovely", "Thandi");

        assertThrows(ConflictException.class, () -> service.review(request));

        when(orderRepository.findByOrderNumber("GFC-1")).thenReturn(Optional.of(order(OrderStatus.DELIVERED)));
        when(itemRepository.findByOrderId("o1")).thenReturn(List.of(item("c1", "v1")));
        assertThrows(BadRequestException.class, () -> service.review(request));
        verify(reviewService, never()).create(anyString(), anyString(), any());
    }

    @Test
    void aWrongOrMissingTokenIsRejected() {
        when(orderRepository.findByOrderNumber("GFC-1")).thenReturn(Optional.of(order(OrderStatus.DELIVERED)));
        when(orderLinks.matches(any(), eq("wrong"))).thenReturn(false);
        when(marketingLinks.matches(EMAIL, "wrong")).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.reorder("GFC-1", "wrong"));
        assertThrows(BadRequestException.class, () -> service.unsubscribe(EMAIL, "wrong", "Unsubscribe page"));
        verify(consent, never()).unsubscribe(anyString(), anyString());
    }

    private static CustomerOrder order(OrderStatus status) {
        return new CustomerOrder.Builder().setOrderId("o1").setOrderNumber("GFC-1").setCustomerEmail(EMAIL)
                .setStatus(status).build();
    }

    private static OrderItem item(String cookieId, String variantId) {
        return new OrderItem.Builder().setOrderId("o1").setCookieId(cookieId).setVariantId(variantId)
                .setQuantity(1).setUnitsPerPack(1).setLineTotal(BigDecimal.TEN).build();
    }
}
