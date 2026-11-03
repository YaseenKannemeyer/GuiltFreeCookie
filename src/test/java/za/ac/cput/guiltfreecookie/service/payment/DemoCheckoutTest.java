package za.ac.cput.guiltfreecookie.service.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.dto.cart.AddItemRequest;
import za.ac.cput.guiltfreecookie.dto.order.CheckoutRequest;
import za.ac.cput.guiltfreecookie.repository.*;
import za.ac.cput.guiltfreecookie.service.cart.ShopCartService;
import za.ac.cput.guiltfreecookie.service.order.CheckoutService;
import za.ac.cput.guiltfreecookie.service.order.OrderService;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-checkout;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop", "app.upload.root-dir=target/test-demo-uploads",
        "app.mail.delivery-check-ms=3600000", "app.mail.cleanup-ms=3600000"
})
@ActiveProfiles("demo")
@AutoConfigureMockMvc
class DemoCheckoutTest {
    @Autowired MockMvc mvc;
    @Autowired CookieRepository cookies;
    @Autowired CookieVariantRepository variants;
    @Autowired CustomerOrderRepository orders;
    @Autowired EmailDeliveryRepository deliveries;
    @Autowired ShopCartService carts;
    @Autowired CheckoutService checkout;
    @Autowired OrderService orderService;

    @Test
    void samplePurchaseRequiresAPrivateTokenAndCapturesConfirmationWithoutSmtp() throws Exception {
        assertEquals(12, cookies.count());
        var cookie = cookies.findBySlug("double-chocolate-chunk").orElseThrow();
        var pack = variants.findByCookieIdOrderBySortOrderAscUnitsPerPackAsc(cookie.getCookieId()).stream()
                .filter(v -> v.getUnitsPerPack() == 6).findFirst().orElseThrow();
        int stock = cookie.getStockQuantity();
        var cart = carts.create(null);
        carts.addItem(cart.cartId(), new AddItemRequest(pack.getVariantId(), 1));
        var result = checkout.checkout(new CheckoutRequest(cart.cartId(), "buyer@example.com", "Demo", "Buyer", null,
                ShippingMethod.PICKUP, null, null, null, false), null);
        assertEquals("DEMO", result.payment().gateway());
        assertEquals("/demo-payment", result.payment().actionUrl());
        mvc.perform(post("/api/v1/orders/" + result.orderNumber() + "/demo-payment").param("token", "wrong"))
                .andExpect(status().isNotFound());
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/orders/" + result.orderNumber() + "/demo-payment").param("token", result.accessToken()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        }
        assertEquals(stock - 6, cookies.findById(cookie.getCookieId()).orElseThrow().getStockQuantity());
        assertEquals(1, deliveries.findAll().stream().filter(d -> d.getSubject().contains(result.orderNumber())).count());
        mvc.perform(get("/api/v1/admin/email-deliveries")).andExpect(status().isUnauthorized());
        orderService.cancelPaid(result.orderNumber(), result.accessToken());
        assertEquals(OrderStatus.REFUNDED, orders.findByOrderNumber(result.orderNumber()).orElseThrow().getStatus());
        assertEquals(stock, cookies.findById(cookie.getCookieId()).orElseThrow().getStockQuantity());
    }
}
