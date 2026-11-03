package za.ac.cput.guiltfreecookie.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import za.ac.cput.guiltfreecookie.domain.Admin;
import za.ac.cput.guiltfreecookie.domain.Customer;
import za.ac.cput.guiltfreecookie.repository.AdminRepository;
import za.ac.cput.guiltfreecookie.repository.CustomerRepository;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests carrying "Authorization: Bearer <jwt>". A valid
 * signature isn't enough on its own: the admin must still exist and be
 * active, and the token must carry their current token version (bumped on
 * every password change or reset), so deactivating an admin or changing
 * their password ends every session they had straight away. Customer tokens
 * carry role=CUSTOMER and follow the same rules against the customers table.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AdminRepository adminRepository;
    private final CustomerRepository customerRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService, AdminRepository adminRepository, CustomerRepository customerRepository) {
        this.jwtService = jwtService;
        this.adminRepository = adminRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            jwtService.parse(header.substring(BEARER_PREFIX.length())).ifPresent(claims -> {
                if (JwtService.ROLE_CUSTOMER.equals(claims.get(JwtService.ROLE_CLAIM))) {
                    customerRepository.findById(claims.getSubject())
                            .filter(customer -> isStillValid(customer, claims))
                            .ifPresent(customer -> authenticate(customer.getCustomerEmail(), "ROLE_CUSTOMER"));
                } else {
                    adminRepository.findById(claims.getSubject())
                            .filter(admin -> isStillValid(admin, claims))
                            .ifPresent(admin -> authenticate(admin.getAdminId(), "ROLE_ADMIN"));
                }
            });
        }

        filterChain.doFilter(request, response);
    }

    private static void authenticate(String principal, String role) {
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    static boolean isStillValid(Admin admin, Claims claims) {
        return admin.isActive() && tokenVersion(claims) == admin.getTokenVersion();
    }

    static boolean isStillValid(Customer customer, Claims claims) {
        return customer.hasAccount() && tokenVersion(claims) == customer.getTokenVersion();
    }

    // Tokens from before versioning carry no claim: they count as version 0.
    private static int tokenVersion(Claims claims) {
        Object version = claims.get(JwtService.VERSION_CLAIM);
        return version instanceof Number number ? number.intValue() : 0;
    }
}
