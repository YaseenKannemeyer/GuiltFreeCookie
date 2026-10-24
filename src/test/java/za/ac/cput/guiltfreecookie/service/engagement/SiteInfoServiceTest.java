package za.ac.cput.guiltfreecookie.service.engagement;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.dto.engagement.RatingSummary;
import za.ac.cput.guiltfreecookie.dto.engagement.SiteInfo;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SiteInfoServiceTest {

    @Test
    void publicInfoUsesConfiguredContactsAndLiveStats() {
        ReviewService reviewService = mock(ReviewService.class);
        CustomerOrderRepository orderRepository = mock(CustomerOrderRepository.class);
        OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
        when(reviewService.overall()).thenReturn(new RatingSummary(4.5, 2));
        when(orderRepository.countCustomers(OrderStatus.PAID_STATUSES)).thenReturn(37L);
        when(orderItemRepository.countCookies(OrderStatus.PAID_STATUSES)).thenReturn(512L);
        SiteInfoService service = new SiteInfoService(
                new MockEnvironment().withProperty("app.site.support-email", "shop@example.com"), reviewService,
                orderRepository, orderItemRepository);

        SiteInfo info = service.info();

        assertEquals("shop@example.com", info.supportEmail());
        assertEquals("", info.phone());
        assertEquals(2, info.stats().reviewCount());
        assertEquals(4.5, info.stats().averageRating());
        assertEquals(37L, info.stats().customersServed());
        assertEquals(512L, info.stats().cookiesSold());
    }
}
