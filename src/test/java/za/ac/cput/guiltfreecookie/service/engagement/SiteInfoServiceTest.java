package za.ac.cput.guiltfreecookie.service.engagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import za.ac.cput.guiltfreecookie.dto.engagement.RatingSummary;

class SiteInfoServiceTest {

    @Test
    void publicInfoUsesConfiguredContactsAndRealStats() {
        var reviews = mock(ReviewService.class);
        when(reviews.overall()).thenReturn(new RatingSummary(4.5, 2));
        var service = new SiteInfoService(
            new MockEnvironment().withProperty(
                "app.site.support-email",
                "shop@example.com"
            ),
            reviews
        );
        var info = service.info();
        assertEquals("shop@example.com", info.get("supportEmail"));
        var stats = (Map<?, ?>) info.get("stats");
        assertEquals(2L, stats.get("reviewCount"));
        assertNull(stats.get("customersServed"));
        assertNull(stats.get("cookiesSold"));
    }
}
