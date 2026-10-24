package za.ac.cput.guiltfreecookie.service.engagement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Admin;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.Customer;
import za.ac.cput.guiltfreecookie.domain.Name;
import za.ac.cput.guiltfreecookie.domain.ReviewStatus;
import za.ac.cput.guiltfreecookie.repository.AdminRepository;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CustomerRepository;
import za.ac.cput.guiltfreecookie.repository.ReviewRepository;
import za.ac.cput.guiltfreecookie.security.JwtService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The whole review journey through the real app: write, approve, see it count, reject.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EngagementFlowTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CookieRepository cookieRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private JwtService jwtService;

    @Test
    void aReviewOnlyCountsInTheCatalogOnceApproved() throws Exception {
        Cookie cookie = cookieRepository.saveAndFlush(new Cookie.Builder()
                .setDescription("Flow cookie").setSlug("flow-cookie").setPrice(34.0).setStockQuantity(8).build());
        Customer customer = customerRepository.saveAndFlush(new Customer.Builder()
                .setCustomerEmail("flow@example.test")
                .setName(new Name.Builder().setFirstName("Sam").setLastName("Baker").build())
                .setPasswordHash("test-account-hash")
                .build());
        Admin admin = adminRepository.saveAndFlush(new Admin.Builder()
                .setEmail("flow-admin@example.test").setFirstName("Alex").setLastName("Baker")
                .setPassword("hash").setActive(true).build());
        String customerToken = "Bearer " + jwtService.generateToken(customer);
        String adminToken = "Bearer " + jwtService.generateToken(admin);

        mvc.perform(post("/api/v1/me/reviews/" + cookie.getCookieId())
                        .header(HttpHeaders.AUTHORIZATION, customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"title\":\"Delicious\",\"body\":\"A wonderful chocolate cookie.\",\"authorName\":\"Sam\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.customerEmail").doesNotExist());
        mvc.perform(get("/api/v1/cookies/flow-cookie"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewCount").value(0));

        String reviewId = reviewRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.PENDING).getFirst().getId();
        mvc.perform(put("/api/v1/admin/reviews/" + reviewId + "/approve").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/cookies/flow-cookie"))
                .andExpect(jsonPath("$.reviewCount").value(1))
                .andExpect(jsonPath("$.averageRating").value(5.0));
        mvc.perform(get("/api/v1/cookies/flow-cookie/reviews"))
                .andExpect(jsonPath("$.summary.reviewCount").value(1))
                .andExpect(jsonPath("$.page.items[0].customerEmail").doesNotExist());
        mvc.perform(get("/api/v1/site/info"))
                .andExpect(jsonPath("$.stats.reviewCount").value(1))
                .andExpect(jsonPath("$.stats.customersServed").isEmpty());

        mvc.perform(put("/api/v1/admin/reviews/" + reviewId + "/reject").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/cookies/flow-cookie"))
                .andExpect(jsonPath("$.reviewCount").value(0));
    }
}
