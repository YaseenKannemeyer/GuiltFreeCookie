package za.ac.cput.guiltfreecookie.service.engagement;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import za.ac.cput.guiltfreecookie.dto.engagement.RatingSummary;
import za.ac.cput.guiltfreecookie.dto.engagement.SiteInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SiteInfoServiceTest {

    @Test
    void publicInfoUsesConfiguredContactsAndRealStats() {
        ReviewService reviewService = mock(ReviewService.class);
        when(reviewService.overall()).thenReturn(new RatingSummary(4.5, 2));
        SiteInfoService service = new SiteInfoService(
                new MockEnvironment().withProperty("app.site.support-email", "shop@example.com"), reviewService);

        SiteInfo info = service.info();

        assertEquals("shop@example.com", info.supportEmail());
        assertEquals("", info.phone());
        assertEquals(2, info.stats().reviewCount());
        assertEquals(4.5, info.stats().averageRating());
        assertNull(info.stats().customersServed());
        assertNull(info.stats().cookiesSold());
    }
}
