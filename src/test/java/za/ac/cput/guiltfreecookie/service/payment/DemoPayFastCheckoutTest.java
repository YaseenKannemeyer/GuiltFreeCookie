package za.ac.cput.guiltfreecookie.service.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.dto.cart.AddItemRequest;
import za.ac.cput.guiltfreecookie.dto.order.CheckoutRequest;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CookieVariantRepository;
import za.ac.cput.guiltfreecookie.service.cart.ShopCartService;
import za.ac.cput.guiltfreecookie.service.order.CheckoutService;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-payfast-checkout;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop", "app.upload.root-dir=target/test-demo-payfast-uploads",
        "app.mail.delivery-check-ms=3600000", "app.mail.cleanup-ms=3600000",
        "app.demo.simulated-payments=false"
})
@ActiveProfiles("demo")
@AutoConfigureMockMvc
class DemoPayFastCheckoutTest {
    @Autowired MockMvc mvc;
    @Autowired CookieRepository cookies;
    @Autowired CookieVariantRepository variants;
    @Autowired ShopCartService carts;
    @Autowired CheckoutService checkout;

    @Test
    void demoCanSendCheckoutToThePayFastSandboxInsteadOfTheSimulatedPage() throws Exception {
        var cookie = cookies.findBySlug("double-chocolate-chunk").orElseThrow();
        var variant = variants.findByCookieIdOrderBySortOrderAscUnitsPerPackAsc(cookie.getCookieId()).getFirst();
        var cart = carts.create(null);
        carts.addItem(cart.cartId(), new AddItemRequest(variant.getVariantId(), 1));
        var result = checkout.checkout(new CheckoutRequest(cart.cartId(), "buyer@example.com", "Demo", "Buyer", null,
                ShippingMethod.PICKUP, null, null, null, false), null);

        assertEquals("PAYFAST", result.payment().gateway());
        assertEquals("https://sandbox.payfast.co.za/eng/process", result.payment().actionUrl());
        mvc.perform(post("/api/v1/orders/" + result.orderNumber() + "/demo-payment").param("token", result.accessToken()))
                .andExpect(status().isNotFound());
    }
}
