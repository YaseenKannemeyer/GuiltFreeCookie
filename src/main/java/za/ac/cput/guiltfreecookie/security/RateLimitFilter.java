package za.ac.cput.guiltfreecookie.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window rate limiting per client IP. Login, customer auth and newsletter sign-up get
 * tight limits (brute force and spam); every other API call shares a
 * generous one so a single client can't hammer the server. Uploaded images
 * are exempt: a product page legitimately loads a dozen at once.
 *
 * Counters live in memory, so limits are per instance and reset on restart.
 * That's enough for a single server; a multi-instance deployment would need
 * a shared store (Redis) instead.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int SWEEP_THRESHOLD = 10_000;

    record Rule(String name, HttpMethod method, String path, int limit, long windowMillis) {
        boolean matches(HttpMethod requestMethod, String requestPath) {
            boolean methodMatches = method == null || method.equals(requestMethod);
            boolean pathMatches = path == null || path.equals(requestPath);
            return methodMatches && pathMatches;
        }
    }

    private static final class Window {
        final long start;
        final long windowMillis;
        final AtomicInteger count = new AtomicInteger();

        Window(long start, long windowMillis) {
            this.start = start;
            this.windowMillis = windowMillis;
        }

        boolean expired(long now) {
            return now - start >= windowMillis;
        }
    }

    private final List<Rule> rules;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitFilter(
            @Value("${app.rate-limit.login.requests}") int loginRequests,
            @Value("${app.rate-limit.login.window-seconds}") long loginWindowSeconds,
            @Value("${app.rate-limit.newsletter.requests}") int newsletterRequests,
            @Value("${app.rate-limit.newsletter.window-seconds}") long newsletterWindowSeconds,
            @Value("${app.rate-limit.customer-auth.requests}") int customerAuthRequests,
            @Value("${app.rate-limit.customer-auth.window-seconds}") long customerAuthWindowSeconds,
            @Value("${app.rate-limit.cart-create.requests}") int cartCreateRequests,
            @Value("${app.rate-limit.cart-create.window-seconds}") long cartCreateWindowSeconds,
            @Value("${app.rate-limit.contact.requests}") int contactRequests,
            @Value("${app.rate-limit.contact.window-seconds}") long contactWindowSeconds,
            @Value("${app.rate-limit.checkout.requests}") int checkoutRequests,
            @Value("${app.rate-limit.checkout.window-seconds}") long checkoutWindowSeconds,
            @Value("${app.rate-limit.review-link.requests}") int reviewLinkRequests,
            @Value("${app.rate-limit.review-link.window-seconds}") long reviewLinkWindowSeconds,
            @Value("${app.rate-limit.api.requests}") int apiRequests,
            @Value("${app.rate-limit.api.window-seconds}") long apiWindowSeconds) {
        this(List.of(
                new Rule("login", HttpMethod.POST, "/admin/login", loginRequests, loginWindowSeconds * 1000),
                new Rule("newsletter", HttpMethod.POST, "/newsletter/subscribe", newsletterRequests, newsletterWindowSeconds * 1000),
                // Same name, so all customer auth endpoints share one counter per client.
                new Rule("customer-auth", HttpMethod.POST, "/api/v1/auth/login", customerAuthRequests, customerAuthWindowSeconds * 1000),
                new Rule("customer-auth", HttpMethod.POST, "/api/v1/auth/register", customerAuthRequests, customerAuthWindowSeconds * 1000),
                new Rule("customer-auth", HttpMethod.POST, "/api/v1/auth/forgot-password", customerAuthRequests, customerAuthWindowSeconds * 1000),
                new Rule("customer-auth", HttpMethod.POST, "/api/v1/auth/reset-password", customerAuthRequests, customerAuthWindowSeconds * 1000),
                new Rule("cart-create", HttpMethod.POST, "/api/v1/carts", cartCreateRequests, cartCreateWindowSeconds * 1000),
                new Rule("contact", HttpMethod.POST, "/api/v1/contact", contactRequests, contactWindowSeconds * 1000),
                new Rule("order-claim", HttpMethod.POST, "/api/v1/me/orders/claim", 3, 3600_000),
                new Rule("checkout", HttpMethod.POST, "/api/v1/checkout", checkoutRequests, checkoutWindowSeconds * 1000),
                new Rule("review-link", HttpMethod.POST, "/api/v1/reviews/by-link", reviewLinkRequests, reviewLinkWindowSeconds * 1000),
                new Rule("api", null, null, apiRequests, apiWindowSeconds * 1000)
        ), Clock.systemUTC());
    }

    RateLimitFilter(List<Rule> rules, Clock clock) {
        this.rules = rules;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod()) || pathOf(request).startsWith("/uploads/")
                || pathOf(request).equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        String path = pathOf(request);
        String client = request.getRemoteAddr();
        long now = clock.millis();

        for (Rule rule : rules) {
            if (!rule.matches(method, path)) {
                continue;
            }
            long retryAfterSeconds = acquire(rule, client, now);
            if (retryAfterSeconds > 0) {
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("Too many requests. Try again in " + retryAfterSeconds + " seconds.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Counts the request; returns 0 if allowed, otherwise seconds until the window resets. */
    private long acquire(Rule rule, String client, long now) {
        if (windows.size() > SWEEP_THRESHOLD) {
            windows.values().removeIf(window -> window.expired(now));
        }

        Window window = windows.compute(rule.name() + '|' + client, (key, existing) ->
                existing == null || existing.expired(now) ? new Window(now, rule.windowMillis()) : existing);

        if (window.count.incrementAndGet() <= rule.limit()) {
            return 0;
        }
        long remainingMillis = window.start + window.windowMillis - now;
        return Math.max(1, (remainingMillis + 999) / 1000);
    }

    private static String pathOf(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
