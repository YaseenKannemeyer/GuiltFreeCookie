package za.ac.cput.guiltfreecookie.service.payment;

import org.junit.jupiter.api.Test;
import za.ac.cput.guiltfreecookie.service.order.OrderLinks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayFastGatewayTest {

    private final OrderLinks links = new OrderLinks("secret", "https://shop.example");

    @Test
    void livePaymentsNeedAPassphrase() {
        assertThrows(IllegalStateException.class, () -> new PayFastGateway(links, false, "10054858", "key", " ", "https://shop.example/notify"));
        assertThrows(IllegalStateException.class, () -> new PayFastGateway(links, false, "10054858", "key", null, "https://shop.example/notify"));
        assertDoesNotThrow(() -> new PayFastGateway(links, false, "10054858", "key", "a-real-passphrase", "https://shop.example/notify"));
        assertDoesNotThrow(() -> new PayFastGateway(links, true, "10000100", "key", "", "https://shop.example/notify"));
    }
}
