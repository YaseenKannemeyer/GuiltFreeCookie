package za.ac.cput.guiltfreecookie.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import za.ac.cput.guiltfreecookie.config.SecurityConfig;
import za.ac.cput.guiltfreecookie.controller.AccountController;
import za.ac.cput.guiltfreecookie.controller.AdminCatalogController;
import za.ac.cput.guiltfreecookie.controller.AdminController;
import za.ac.cput.guiltfreecookie.controller.AdminDiscountController;
import za.ac.cput.guiltfreecookie.controller.MyCartController;
import za.ac.cput.guiltfreecookie.controller.ShopCartController;
import za.ac.cput.guiltfreecookie.controller.CatalogController;
import za.ac.cput.guiltfreecookie.controller.AuthController;
import za.ac.cput.guiltfreecookie.controller.CookieController;
import za.ac.cput.guiltfreecookie.controller.CustomerController;
import za.ac.cput.guiltfreecookie.controller.NewsletterController;
import za.ac.cput.guiltfreecookie.domain.Admin;
import za.ac.cput.guiltfreecookie.domain.Customer;
import za.ac.cput.guiltfreecookie.domain.Name;
import za.ac.cput.guiltfreecookie.repository.AdminRepository;
import za.ac.cput.guiltfreecookie.repository.CustomerRepository;
import za.ac.cput.guiltfreecookie.service.AdminService;
import za.ac.cput.guiltfreecookie.service.CookieService;
import za.ac.cput.guiltfreecookie.service.CustomerAuthService;
import za.ac.cput.guiltfreecookie.service.CustomerService;
import za.ac.cput.guiltfreecookie.service.ImageStorageService;
import za.ac.cput.guiltfreecookie.service.NewsletterService;
import za.ac.cput.guiltfreecookie.service.cart.ShopCartService;
import za.ac.cput.guiltfreecookie.service.discount.DiscountService;
import za.ac.cput.guiltfreecookie.service.catalog.CatalogAdminService;
import za.ac.cput.guiltfreecookie.service.catalog.CatalogService;
import za.ac.cput.guiltfreecookie.service.catalog.CookieImageService;
import za.ac.cput.guiltfreecookie.service.catalog.CookieVariantService;
import za.ac.cput.guiltfreecookie.service.catalog.InventoryService;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The real filter chain: who may call what, and which headers come back. */
@WebMvcTest(controllers = {
        za.ac.cput.guiltfreecookie.controller.engagement.EngagementController.class, za.ac.cput.guiltfreecookie.controller.engagement.EmailPreviewController.class, za.ac.cput.guiltfreecookie.controller.engagement.EmailAssetController.class,
        AdminController.class, CookieController.class, CustomerController.class, NewsletterController.class,
        AuthController.class, AccountController.class, CatalogController.class, AdminCatalogController.class,
        ShopCartController.class, MyCartController.class, AdminDiscountController.class})
@Import({SecurityConfig.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=dGVzdC1vbmx5LXNlY3JldC10aGF0LWlzLTMyLWJ5dGVzISE=",
        "app.jwt.expiration-minutes=60",
        "app.cors.allowed-origins=https://guiltfreecookie.co.za",
        "app.security.require-https=false"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AdminRepository adminRepository;
    @MockitoBean
    private AdminService adminService;
    @MockitoBean
    private CookieService cookieService;
    @MockitoBean
    private CustomerService customerService;
    @MockitoBean
    private ImageStorageService imageStorageService;
    @MockitoBean
    private NewsletterService newsletterService;
    @MockitoBean
    private CustomerRepository customerRepository;
    @MockitoBean
    private CustomerAuthService customerAuthService;
    @MockitoBean
    private CatalogService catalogService;
    @MockitoBean
    private CatalogAdminService catalogAdminService;
    @MockitoBean
    private CookieVariantService cookieVariantService;
    @MockitoBean
    private CookieImageService cookieImageService;
    @MockitoBean
    private InventoryService inventoryService;
    @MockitoBean
    private ShopCartService shopCartService;
    @MockitoBean
    private DiscountService discountService;

    @MockitoBean private za.ac.cput.guiltfreecookie.service.engagement.ContactService contacts;
    @MockitoBean private za.ac.cput.guiltfreecookie.service.engagement.SiteInfoService site;
    @MockitoBean private za.ac.cput.guiltfreecookie.service.engagement.ReviewService reviews;
    @MockitoBean private za.ac.cput.guiltfreecookie.service.notification.EmailTemplates templates;
    @MockitoBean private za.ac.cput.guiltfreecookie.service.notification.EmailService email;
    @MockitoBean private za.ac.cput.guiltfreecookie.service.notification.EmailAssetService assets;

    private final Customer thandi = new Customer.Builder()
            .setCustomerEmail("thandi@example.co.za")
            .setName(new Name.Builder().setFirstName("Thandi").setLastName("Mokoena").build())
            .setPasswordHash("$2a$10$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz012")
            .setTokenVersion(0)
            .build();

    private final Admin jane = new Admin.Builder()
            .setAdminId("AD001")
            .setFirstName("Jane")
            .setLastName("Doe")
            .setEmail("jane@guiltfreecookie.com")
            .setPassword("$2a$10$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz012")
            .setActive(true)
            .build();

    private String bearer(Admin admin) {
        return "Bearer " + jwtService.generateToken(admin);
    }

    private static RequestPostProcessor from(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    @Test
    void theCatalogueIsPublic() throws Exception {
        when(cookieService.getAllActive()).thenReturn(List.of());

        mvc.perform(get("/cookie/getAll")).andExpect(status().isOk());
    }

    @Test
    void customerRecordsNeedAnAdmin() throws Exception {
        mvc.perform(get("/api/v1/customers")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/customers/someone@example.com")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointsNeedAToken() throws Exception {
        mvc.perform(get("/admin/getAll")).andExpect(status().isUnauthorized());
        mvc.perform(get("/cookie/getAllForAdmin")).andExpect(status().isUnauthorized());
        mvc.perform(get("/newsletter/subscribers")).andExpect(status().isUnauthorized());
    }

    @Test
    void aValidTokenGetsInAndPasswordHashesNeverComeBack() throws Exception {
        when(adminRepository.findById("AD001")).thenReturn(Optional.of(jane));
        when(adminService.getAll()).thenReturn(List.of(jane));

        mvc.perform(get("/admin/getAll").header(HttpHeaders.AUTHORIZATION, bearer(jane)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jane@guiltfreecookie.com")))
                .andExpect(content().string(not(containsString("\"password\""))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void aDeactivatedAdminsTokenStopsWorking() throws Exception {
        String token = bearer(jane);
        when(adminRepository.findById("AD001"))
                .thenReturn(Optional.of(new Admin.Builder().copy(jane).setActive(false).build()));

        mvc.perform(get("/admin/getAll").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenFromBeforeAPasswordChangeStopsWorking() throws Exception {
        String token = bearer(jane);
        when(adminRepository.findById("AD001"))
                .thenReturn(Optional.of(new Admin.Builder().copy(jane).setTokenVersion(1).build()));

        mvc.perform(get("/admin/getAll").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aForgedTokenIsRejected() throws Exception {
        JwtService attacker = new JwtService("YXR0YWNrZXItY2hvc2VuLXNlY3JldC10aGF0LWlzLTMyLWJ5dGVz", 60);
        String forged = "Bearer " + attacker.generateToken(jane);

        mvc.perform(get("/admin/getAll").header(HttpHeaders.AUTHORIZATION, forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void newsletterSignUpIsPublic() throws Exception {
        mvc.perform(post("/newsletter/subscribe").with(from("198.51.100.20"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"thandi@example.co.za\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void responsesCarrySecurityHeaders() throws Exception {
        when(cookieService.getAllActive()).thenReturn(List.of());

        mvc.perform(get("/cookie/getAll"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", containsString("camera=()")));
    }

    @Test
    void corsAllowsOnlyTheConfiguredOrigin() throws Exception {
        mvc.perform(options("/cookie/getAll")
                        .header(HttpHeaders.ORIGIN, "https://guiltfreecookie.co.za")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://guiltfreecookie.co.za"));

        mvc.perform(options("/cookie/getAll")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginIsRateLimited() throws Exception {
        String body = "{\"email\":\"jane@guiltfreecookie.com\",\"password\":\"wrong-password\"}";
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/admin/login").with(from("203.0.113.99"))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(post("/admin/login").with(from("203.0.113.99"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void customerAuthEndpointsArePublic() throws Exception {
        mvc.perform(post("/api/v1/auth/forgot-password").with(from("198.51.100.30"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"thandi@example.co.za\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void myAccountNeedsACustomerToken() throws Exception {
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void aCustomerTokenOpensMyAccountAndHidesThePasswordHash() throws Exception {
        when(customerRepository.findById("thandi@example.co.za")).thenReturn(Optional.of(thandi));
        when(customerAuthService.getProfile("thandi@example.co.za")).thenReturn(thandi);

        mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(thandi)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("thandi@example.co.za")))
                .andExpect(content().string(not(containsString("$2a$"))))
                .andExpect(content().string(not(containsString("passwordHash"))));
    }

    @Test
    void aCustomerTokenCannotReachAdminEndpoints() throws Exception {
        when(customerRepository.findById("thandi@example.co.za")).thenReturn(Optional.of(thandi));
        String token = "Bearer " + jwtService.generateToken(thandi);

        mvc.perform(get("/admin/getAll").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/customers").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
    }

    @Test
    void anAdminTokenIsNotACustomerSession() throws Exception {
        when(adminRepository.findById("AD001")).thenReturn(Optional.of(jane));

        mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(jane)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aCustomerTokenStopsWorkingAfterAPasswordChange() throws Exception {
        String token = "Bearer " + jwtService.generateToken(thandi);
        when(customerRepository.findById("thandi@example.co.za"))
                .thenReturn(Optional.of(new Customer.Builder().copy(thandi).setTokenVersion(1).build()));

        mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerAuthEndpointsShareOneRateLimit() throws Exception {
        String body = "{\"email\":\"thandi@example.co.za\",\"password\":\"wrong-password\"}";
        for (int i = 0; i < 10; i++) {
            mvc.perform(post(i % 2 == 0 ? "/api/v1/auth/login" : "/api/v1/auth/forgot-password")
                    .with(from("203.0.113.77")).contentType(MediaType.APPLICATION_JSON).content(body));
        }

        mvc.perform(post("/api/v1/auth/register").with(from("203.0.113.77"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void theNewCatalogIsPublicForReading() throws Exception {
        mvc.perform(get("/api/v1/cookies")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/cookies/categories")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/cookies/double-chocolate-chunk")).andExpect(status().isOk());
    }

    @Test
    void catalogManagementNeedsAnAdmin() throws Exception {
        when(customerRepository.findById("thandi@example.co.za")).thenReturn(Optional.of(thandi));
        String customerToken = "Bearer " + jwtService.generateToken(thandi);
        String stock = "{\"adjustBy\":10}";

        mvc.perform(post("/api/v1/cookies").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/cookies")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/admin/cookies/CK1/stock").contentType(MediaType.APPLICATION_JSON).content(stock))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/admin/cookies/CK1/stock").header(HttpHeaders.AUTHORIZATION, customerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(stock))
                .andExpect(status().isForbidden());

        when(adminRepository.findById("AD001")).thenReturn(Optional.of(jane));
        mvc.perform(put("/api/v1/admin/cookies/CK1/stock").header(HttpHeaders.AUTHORIZATION, bearer(jane))
                        .contentType(MediaType.APPLICATION_JSON).content(stock))
                .andExpect(status().isOk());
    }

    @Test
    void theCartWorksForGuests() throws Exception {
        mvc.perform(post("/api/v1/carts").with(from("198.51.100.40"))).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/carts/some-cart-id")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/carts/some-cart-id/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":\"V1\",\"quantity\":1}")).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/carts/some-cart-id/discount")).andExpect(status().isOk());
    }

    @Test
    void aSignedInCustomerIsRecognisedWhenStartingACart() throws Exception {
        when(customerRepository.findById("thandi@example.co.za")).thenReturn(Optional.of(thandi));

        mvc.perform(post("/api/v1/carts").with(from("198.51.100.41"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(thandi)))
                .andExpect(status().isCreated());
        org.mockito.Mockito.verify(shopCartService).create("thandi@example.co.za");

        mvc.perform(post("/api/v1/carts").with(from("198.51.100.42"))).andExpect(status().isCreated());
        org.mockito.Mockito.verify(shopCartService).create(null);
    }

    @Test
    void mySavedCartNeedsACustomerToken() throws Exception {
        mvc.perform(get("/api/v1/me/cart")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/me/cart/merge").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void discountCodesAreManagedByAdminsOnly() throws Exception {
        when(customerRepository.findById("thandi@example.co.za")).thenReturn(Optional.of(thandi));

        mvc.perform(get("/api/v1/admin/discounts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/discounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(thandi)))
                .andExpect(status().isForbidden());
    }

    @Test
    void startingCartsIsRateLimited() throws Exception {
        for (int i = 0; i < 30; i++) {
            mvc.perform(post("/api/v1/carts").with(from("203.0.113.55"))).andExpect(status().isCreated());
        }
        mvc.perform(post("/api/v1/carts").with(from("203.0.113.55"))).andExpect(status().isTooManyRequests());
    }

    @Test void engagementPublicReadsAndContactArePublic() throws Exception {
        for(String path:List.of("/api/v1/site/info","/api/v1/reviews/featured","/api/v1/cookies/chocolate/reviews"))
            mvc.perform(get(path)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/contact").contentType(MediaType.APPLICATION_JSON).content("{\"website\":\"bot\"}"))
                .andExpect(status().isNoContent());
    }
    @Test void reviewCreationRequiresCustomerAndAdminToolsRequireAdmin() throws Exception {
        mvc.perform(post("/api/v1/me/reviews/c1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        when(customerRepository.findById(thandi.getCustomerEmail())).thenReturn(Optional.of(thandi));
        String customerToken="Bearer "+jwtService.generateToken(thandi);
        mvc.perform(post("/api/v1/me/reviews/c1").header(HttpHeaders.AUTHORIZATION,customerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}")) .andExpect(status().isCreated());
        for(String path:List.of("/api/v1/admin/messages","/api/v1/admin/reviews","/api/v1/admin/email-previews")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION,customerToken)).andExpect(status().isForbidden());
        }
        when(adminRepository.findById(jane.getAdminId())).thenReturn(Optional.of(jane));
        mvc.perform(post("/api/v1/me/reviews/c1").header(HttpHeaders.AUTHORIZATION,bearer(jane)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/email-previews").header(HttpHeaders.AUTHORIZATION,bearer(jane))).andExpect(status().isOk());
    }
    @Test void contactIsLimitedToFivePerHourPerIp() throws Exception {
        for(int i=0;i<5;i++) mvc.perform(post("/api/v1/contact").with(from("192.0.2.66"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")) .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/contact").with(from("192.0.2.66")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
    }

    @Test void adminEngagementWritesRejectGuestsAndCustomers() throws Exception {
        when(customerRepository.findById(thandi.getCustomerEmail())).thenReturn(Optional.of(thandi));
        String customerToken="Bearer "+jwtService.generateToken(thandi);
        for(String path:List.of("/api/v1/admin/messages/m1/resolve", "/api/v1/admin/reviews/r1/approve", "/api/v1/admin/reviews/r1/reject")) {
            mvc.perform(put(path)).andExpect(status().isUnauthorized());
            mvc.perform(put(path).header(HttpHeaders.AUTHORIZATION,customerToken)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/admin/email-previews/password-reset/send-test")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/email-previews/password-reset/send-test").header(HttpHeaders.AUTHORIZATION,customerToken)).andExpect(status().isForbidden());
    }
    @Test void testEmailRecipientComesFromAuthenticatedAdmin() throws Exception {
        when(adminRepository.findById(jane.getAdminId())).thenReturn(Optional.of(jane));
        when(templates.sample("password-reset")).thenReturn(java.util.Map.of("actionUrl","https://example.test"));
        mvc.perform(post("/api/v1/admin/email-previews/password-reset/send-test").header(HttpHeaders.AUTHORIZATION,bearer(jane)))
                .andExpect(status().isAccepted());
        org.mockito.Mockito.verify(email).sendTemplate(org.mockito.ArgumentMatchers.eq(jane.getEmail()),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.eq("password-reset"),org.mockito.ArgumentMatchers.anyMap(),org.mockito.ArgumentMatchers.eq(false));
    }
    @Test void emailAssetsArePublicButUnknownCookiesStayNotFound() throws Exception {
        when(assets.image("missing")).thenThrow(new za.ac.cput.guiltfreecookie.exception.NotFoundException("Cookie not found"));
        mvc.perform(get("/api/v1/email-assets/cookies/missing.png")).andExpect(status().isNotFound());
    }
}
