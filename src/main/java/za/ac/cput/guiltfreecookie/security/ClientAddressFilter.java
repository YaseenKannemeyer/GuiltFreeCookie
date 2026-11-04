package za.ac.cput.guiltfreecookie.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Azure's front end sends X-Forwarded-For as "ip:port" and Tomcat keeps the port, so getRemoteAddr()
// would never match an IP. Runs first so rate limiting and the PayFast check see the bare address.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ClientAddressFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String original = request.getRemoteAddr();
        String bare = withoutPort(original);
        if (bare.equals(original)) {
            chain.doFilter(request, response);
            return;
        }
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getRemoteAddr() {
                return bare;
            }

            @Override
            public String getRemoteHost() {
                return bare;
            }
        }, response);
    }

    // "1.2.3.4:5678" -> "1.2.3.4", "[2001:db8::1]:443" -> "2001:db8::1"; a bare IPv6 address is left alone.
    static String withoutPort(String address) {
        if (address == null) {
            return "";
        }
        String value = address.trim();
        if (value.startsWith("[")) {
            int end = value.indexOf(']');
            return end > 0 ? value.substring(1, end) : value;
        }
        int colon = value.indexOf(':');
        if (colon > 0 && colon == value.lastIndexOf(':')) {
            return value.substring(0, colon);
        }
        return value;
    }
}
