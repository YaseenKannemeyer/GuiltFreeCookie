package za.ac.cput.guiltfreecookie.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import za.ac.cput.guiltfreecookie.security.JwtAuthenticationFilter;
import za.ac.cput.guiltfreecookie.security.RateLimitFilter;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final List<String> allowedOrigins;
    private final boolean requireHttps;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitFilter rateLimitFilter,
            @Value("${app.cors.allowed-origins}") String allowedOrigins,
            @Value("${app.security.require-https}") boolean requireHttps) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        this.requireHttps = requireHttps;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Admins authenticate with JWTs, never with a username/password form.
    // Declaring this stops Spring Boot from creating its default in-memory
    // user with a generated password.
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF needs an ambient credential the browser attaches on its
                // own (a cookie). This API has none: it is stateless and only
                // trusts an Authorization header that our own script adds, which
                // another site cannot make the browser send.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        // The API only ever returns JSON and images: nothing in a
                        // response should run, load or be framed.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(Duration.ofDays(365).toSeconds()))
                        .permissionsPolicyHeader(permissions -> permissions.policy(
                                "camera=(), microphone=(), geolocation=(), payment=()"))
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                )
                .authorizeHttpRequests(auth -> auth
                        // Error pages render the status of a request that was
                        // already authorised (or rejected) on its way in.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/admin/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/cookie/getAll", "/cookie/read/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/cookies", "/api/v1/cookies/**").permitAll()
                        .requestMatchers("/api/v1/carts", "/api/v1/carts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/newsletter/subscribe").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/contact").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/site/info", "/api/v1/reviews/featured", "/api/v1/email-assets/cookies/**", "/email/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/api/v1/email-assets/cookies/**", "/email/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/me", "/api/v1/me/**").hasRole("CUSTOMER")
                        .anyRequest().hasRole("ADMIN")
                )
                .addFilterAfter(rateLimitFilter, CorsFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (requireHttps) {
            http.redirectToHttps(Customizer.withDefaults());
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Retry-After"));
        configuration.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Both filters run inside the security chain above. Without these, Spring
    // Boot would also register them as plain servlet filters (they're
    // @Components), outside the chain and its ordering.
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
