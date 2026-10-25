package za.ac.cput.guiltfreecookie.service.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.*;
import za.ac.cput.guiltfreecookie.dto.account.SavedAddressRequest;
import za.ac.cput.guiltfreecookie.dto.order.AddressRequest;
import za.ac.cput.guiltfreecookie.repository.*;
import za.ac.cput.guiltfreecookie.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountFlowTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CustomerRepository customers;

    @Autowired
    CustomerOrderRepository orders;

    @Autowired
    JwtService jwt;

    @Autowired
    ClaimOrderLinks links;

    @Autowired
    AddressBookService addresses;

    String owner = "phase5@example.com";

    Customer account(String email) {
        return customers.saveAndFlush(
            new Customer.Builder()
                .setCustomerEmail(email)
                .setPasswordHash("test-account")
                .setName(new Name.Builder().setFirstName("Sam").setLastName("Baker").build())
                .build()
        );
    }

    @Test
    void inboxProofUnlocksGuestOrdersAndFlattensDetailWithoutExposingOtherOwners() throws Exception {
        String token = "Bearer " + jwt.generateToken(account(owner));
        orders.saveAndFlush(
            new CustomerOrder.Builder()
                .setOrderNumber("GFC-PHASE5")
                .setCustomerEmail(owner)
                .setAccountOwned(false)
                .setFirstName("Sam")
                .setLastName("Baker")
                .setShippingMethod(ShippingMethod.PICKUP)
                .setStatus(OrderStatus.PAID)
                .setPaidAt(LocalDateTime.now())
                .setCreatedAt(LocalDateTime.now())
                .setSubtotal(BigDecimal.TEN)
                .setDiscountTotal(BigDecimal.ZERO)
                .setShippingTotal(BigDecimal.ZERO)
                .setTotal(BigDecimal.TEN)
                .build()
        );
        mvc.perform(get("/api/v1/me/orders").header("Authorization", token)).andExpect(
            jsonPath("$.totalItems").value(0)
        );
        mvc.perform(get("/api/v1/me/orders/claimable").header("Authorization", token)).andExpect(
            jsonPath("$.count").value(1)
        );
        mvc.perform(get("/api/v1/me/orders/GFC-PHASE5").header("Authorization", token)).andExpect(
            status().isNotFound()
        );
        mvc.perform(
            post("/api/v1/me/orders/claim/confirm")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + links.token(owner) + "\"}")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.claimed").value(1));
        mvc.perform(get("/api/v1/me/orders/GFC-PHASE5").header("Authorization", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderNumber").value("GFC-PHASE5"))
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.order").doesNotExist());
        mvc.perform(get("/api/v1/me/orders").header("Authorization", token)).andExpect(
            jsonPath("$.items[0].orderNumber").value("GFC-PHASE5")
        );
        String other = "Bearer " + jwt.generateToken(account("other-phase5@example.com"));
        mvc.perform(get("/api/v1/me/orders/GFC-PHASE5").header("Authorization", other)).andExpect(
            status().isNotFound()
        );
        mvc.perform(
            post("/api/v1/me/orders/claim/confirm")
                .header("Authorization", other)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + links.token(owner) + "\"}")
        ).andExpect(status().isBadRequest());
    }

    @Test
    void addressBookDefaultsAndOwnershipWorkWithRealPersistence() throws Exception {
        String token = "Bearer " + jwt.generateToken(account(owner));
        var address = new AddressRequest("12", "Bakery Lane", null, "Central", "Pretoria", Province.GAUTENG, "0002");
        var first = addresses.create(owner, new SavedAddressRequest("Home", "Sam Baker", "0821234567", false, address));
        var second = addresses.create(
            owner,
            new SavedAddressRequest("Work", "Sam Baker", "0821234567", false, address)
        );
        assertTrue(first.isDefault());
        assertFalse(second.isDefault());
        addresses.makeDefault(owner, second.id());
        assertEquals(
            second.id(),
            addresses
                .list(owner)
                .stream()
                .filter(a -> a.isDefault())
                .findFirst()
                .orElseThrow()
                .id()
        );
        addresses.delete(owner, second.id());
        assertTrue(addresses.list(owner).getFirst().isDefault());
        mvc.perform(get("/api/v1/me/addresses").header("Authorization", token))
            .andExpect(jsonPath("$[0].isDefault").value(true))
            .andExpect(jsonPath("$[0].address.postalCode").value("0002"))
            .andExpect(jsonPath("$[0].customerEmail").doesNotExist());
        String other = "Bearer " + jwt.generateToken(account("other-phase5@example.com"));
        mvc.perform(delete("/api/v1/me/addresses/" + first.id()).header("Authorization", other)).andExpect(
            status().isNotFound()
        );
        assertEquals(1, addresses.list(owner).size());
    }
}
