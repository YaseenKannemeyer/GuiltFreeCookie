package za.ac.cput.guiltfreecookie.service.shipping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.ac.cput.guiltfreecookie.domain.Province;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipLogicBookingTest {

    private static final DeliveryArea RONDEBOSCH = DeliveryArea.of("Rondebosch", "Cape Town", Province.WESTERN_CAPE, "7700");
    private static final ShipLogicClient.Recipient SAM = new ShipLogicClient.Recipient("Sam Baker", "0821234567",
            "sam@example.com", "Unit 4, 12 Main Road", RONDEBOSCH, "Ring the bell");

    private HttpServer server;
    private final AtomicReference<String> booked = new AtomicReference<>();
    private volatile String bookingAnswer = "{\"id\":129585630,\"short_tracking_reference\":\"S8WW8L\",\"rate\":95}";
    private volatile int bookingStatus = 200;
    private String base;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/rates", exchange -> answer(exchange, 200, """
                {"rates":[{"rate":86.25,"service_level":{"id":11,"code":"LOF"}},{"rate":95,"service_level":{"id":12,"code":"ECO"}}]}"""));
        server.createContext("/v2/shipments", exchange -> {
            String path = exchange.getRequestURI().toString();
            if ("POST".equals(exchange.getRequestMethod())) {
                booked.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                answer(exchange, bookingStatus, bookingAnswer);
            } else if (path.startsWith("/v2/shipments/label?id=129585630")) {
                answer(exchange, 200, "{\"url\":\"https://labels.example/S8WW8L.pdf?Signature=x\"}");
            } else if (path.equals("/v2/shipments?tracking_reference=S8WW8L")) {
                answer(exchange, 200, "{\"shipments\":[{\"id\":129585630}]}");
            } else {
                answer(exchange, 200, "{\"shipments\":[]}");
            }
        });
        server.createContext("/v2/tracking/shipments", exchange -> answer(exchange, 200,
                "{\"shipments\":[{\"short_tracking_reference\":\"S8WW8L\",\"status\":\"delivered\"}]}"));
        server.createContext("/v2/accounts/webhooks", exchange -> answer(exchange, 200, "{\"webhook_subscriptions\":null}"));
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static void answer(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private ShipLogicClient client(String key) {
        return new ShipLogicClient(key, base, "ECO", "https://portal.example/track?ref={ref}",
                new ShipLogicClient.Collection("Guilt Free Cookie", "27 Spine Road", "Strandfontein", "Cape Town",
                        Province.WESTERN_CAPE, "7798", "Guilt Free Cookie", "0215550123", "hello@example.test"),
                new ShipLogicClient.Box(32, 24, 10, 280, 45, 24), HttpClient.newHttpClient());
    }

    @Test
    void booksTheCheapestAllowedServiceWithBothContacts() throws Exception {
        ShipLogicClient.Booking booking = client("key").book("GFC-261106-ABCD", SAM, 48, new BigDecimal("168.00"));

        assertEquals("S8WW8L", booking.trackingReference());
        assertEquals(0, new BigDecimal("95").compareTo(booking.rate()));
        JsonNode sent = new ObjectMapper().readTree(booked.get());
        assertEquals(12, sent.path("service_level_id").asInt());
        assertEquals("Unit 4, 12 Main Road", sent.path("delivery_address").path("street_address").asText());
        assertEquals("27 Spine Road", sent.path("collection_address").path("street_address").asText());
        assertEquals("0821234567", sent.path("delivery_contact").path("mobile_number").asText());
        assertEquals("0215550123", sent.path("collection_contact").path("mobile_number").asText());
        assertEquals("GFC-261106-ABCD", sent.path("customer_reference").asText());
        assertEquals("Ring the bell", sent.path("special_instructions_delivery").asText());
        assertEquals(2, sent.path("parcels").size());
    }

    @Test
    void theCouriersReasonForRefusingReachesTheAdmin() {
        bookingStatus = 400;
        bookingAnswer = "invalid delivery address";
        ShipLogicException error = assertThrows(ShipLogicException.class,
                () -> client("key").book("GFC-261106-ABCD", SAM, 24, BigDecimal.TEN));
        assertEquals("The Courier Guy said: invalid delivery address", error.getMessage());
        assertThrows(ShipLogicException.class, () -> client("").book("GFC-261106-ABCD", SAM, 24, BigDecimal.TEN));
    }

    @Test
    void waybillTrackingAndHealth() {
        ShipLogicClient client = client("key");
        assertEquals("https://labels.example/S8WW8L.pdf?Signature=x", client.waybillUrl("S8WW8L"));
        assertThrows(ShipLogicException.class, () -> client.waybillUrl("NOPE"));
        assertEquals(Optional.of("delivered"), client.status("S8WW8L"));
        assertEquals("https://portal.example/track?ref=S8WW8L", client.trackingUrl("S8WW8L"));
        assertTrue(client.reachable());
        server.stop(0);
        assertFalse(client.reachable());
        assertEquals(Optional.empty(), client.status("S8WW8L"));
    }
}
