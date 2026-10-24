package za.ac.cput.guiltfreecookie.service.engagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.*;
import za.ac.cput.guiltfreecookie.repository.*;
import za.ac.cput.guiltfreecookie.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EngagementFlowTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CookieRepository cookies;

    @Autowired
    CustomerRepository customers;

    @Autowired
    AdminRepository admins;

    @Autowired
    ReviewRepository reviews;

    @Autowired
    JwtService jwt;

    @Test
    void submittedReviewOnlyAffectsPublicCatalogAfterApproval()
        throws Exception {
        Cookie cookie = cookies.saveAndFlush(
            new Cookie.Builder()
                .setDescription("Flow cookie")
                .setSlug("flow-cookie")
                .setPrice(34.0)
                .setStockQuantity(8)
                .build()
        );
        Customer customer = customers.saveAndFlush(
            new Customer.Builder()
                .setCustomerEmail("flow@example.test")
                .setName(
                    new Name.Builder()
                        .setFirstName("Sam")
                        .setLastName("Baker")
                        .build()
                )
                .setPasswordHash("test-account-hash")
                .build()
        );
        Admin admin = admins.saveAndFlush(
            new Admin.Builder()
                .setEmail("flow-admin@example.test")
                .setFirstName("Alex")
                .setLastName("Baker")
                .setPassword("hash")
                .setActive(true)
                .build()
        );
        mvc
            .perform(
                post("/api/v1/me/reviews/" + cookie.getCookieId())
                    .header(
                        "Authorization",
                        "Bearer " + jwt.generateToken(customer)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"rating\":5,\"title\":\"Delicious\",\"body\":\"A wonderful chocolate cookie.\",\"authorName\":\"Sam\"}"
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.customerEmail").doesNotExist());
        mvc
            .perform(get("/api/v1/cookies/flow-cookie"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewCount").value(0));
        String id = reviews
            .findByStatusOrderByCreatedAtDesc(ReviewStatus.PENDING)
            .getFirst()
            .getId();
        mvc
            .perform(
                put("/api/v1/admin/reviews/" + id + "/approve").header(
                    "Authorization",
                    "Bearer " + jwt.generateToken(admin)
                )
            )
            .andExpect(status().isOk());
        mvc
            .perform(get("/api/v1/cookies/flow-cookie"))
            .andExpect(jsonPath("$.reviewCount").value(1))
            .andExpect(jsonPath("$.averageRating").value(5.0));
        mvc
            .perform(get("/api/v1/cookies/flow-cookie/reviews"))
            .andExpect(jsonPath("$.summary.reviewCount").value(1))
            .andExpect(
                jsonPath("$.page.items[0].customerEmail").doesNotExist()
            );
        mvc
            .perform(get("/api/v1/site/info"))
            .andExpect(jsonPath("$.stats.reviewCount").value(1))
            .andExpect(jsonPath("$.stats.customersServed").isEmpty());
        mvc
            .perform(
                put("/api/v1/admin/reviews/" + id + "/reject").header(
                    "Authorization",
                    "Bearer " + jwt.generateToken(admin)
                )
            )
            .andExpect(status().isOk());
        mvc
            .perform(get("/api/v1/cookies/flow-cookie"))
            .andExpect(jsonPath("$.reviewCount").value(0));
    }
}
