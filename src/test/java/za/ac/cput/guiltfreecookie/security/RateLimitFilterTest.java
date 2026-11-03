package za.ac.cput.guiltfreecookie.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RateLimitFilterTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-12T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final RateLimitFilter filter = new RateLimitFilter(List.of(
            new RateLimitFilter.Rule("login", HttpMethod.POST, "/admin/login", 3, 60_000),
            new RateLimitFilter.Rule("api", null, null, 100, 60_000)
    ), clock);

    private MockHttpServletResponse send(String method, String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/guiltFreeCookie" + path);
        request.setContextPath("/guiltFreeCookie");
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void allowsRequestsUpToTheLimitThenAnswers429WithRetryAfter() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertEquals(200, send("POST", "/admin/login", "203.0.113.5").getStatus());
        }

        MockHttpServletResponse blocked = send("POST", "/admin/login", "203.0.113.5");

        assertEquals(429, blocked.getStatus());
        assertNotNull(blocked.getHeader("Retry-After"));
        assertEquals("60", blocked.getHeader("Retry-After"));
    }

    @Test
    void countsEachClientSeparately() throws Exception {
        for (int i = 0; i < 3; i++) {
            send("POST", "/admin/login", "203.0.113.5");
        }

        assertEquals(200, send("POST", "/admin/login", "198.51.100.7").getStatus());
    }

    @Test
    void resetsWhenTheWindowEnds() throws Exception {
        for (int i = 0; i < 4; i++) {
            send("POST", "/admin/login", "203.0.113.5");
        }

        clock.advance(Duration.ofSeconds(61));

        assertEquals(200, send("POST", "/admin/login", "203.0.113.5").getStatus());
    }

    @Test
    void loginLimitDoesNotThrottleOtherEndpoints() throws Exception {
        for (int i = 0; i < 4; i++) {
            send("POST", "/admin/login", "203.0.113.5");
        }

        assertEquals(200, send("GET", "/cookie/getAll", "203.0.113.5").getStatus());
    }

    @Test
    void uploadedImagesAreNeverThrottled() throws Exception {
        for (int i = 0; i < 150; i++) {
            MockHttpServletResponse response = send("GET", "/uploads/cookies/a.png", "203.0.113.5");
            assertNull(response.getHeader("Retry-After"));
        }
    }

    @Test
    void anEncodedPathCountsAgainstTheSameRule() throws Exception {
        for (int i = 0; i < 3; i++) {
            send("POST", "/admin/login", "203.0.113.9");
        }

        assertEquals(429, send("POST", "/admin/logi%6E", "203.0.113.9").getStatus());
        assertEquals(429, send("POST", "/admin/login;jsessionid=x", "203.0.113.9").getStatus());
    }
}
