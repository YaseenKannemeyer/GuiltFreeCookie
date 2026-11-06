package za.ac.cput.guiltfreecookie.service.shipping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipLogicClientTest {

    private static final DeliveryArea PARKTOWN = DeliveryArea.of("Parktown", "Johannesburg", Province.GAUTENG, "2193");
    private static final String RATES = """
            {"rates":[
              {"rate":86.25,"service_level":{"code":"LOF"}},
              {"rate":103.5,"service_level":{"code":"LSE"}},
              {"rate":95,"service_level":{"code":"ECO"}}
            ]}""";

    private HttpServer server;
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> auth = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String answer = RATES;
    private String base;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/rates", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private ShipLogicClient client(String key, String levels) {
        return new ShipLogicClient(key, base + "/", levels, "https://track.example/?ref={ref}",
                new ShipLogicClient.Collection("Guilt Free Cookie", "", "Strandfontein", "Cape Town", Province.WESTERN_CAPE, "7798",
                        "Guilt Free Cookie", "0215550123", "hello@example.test"),
                new ShipLogicClient.Box(30, 20, 12, 250, 60, 24), HttpClient.newHttpClient());
    }

    @Test
    void picksTheCheapestAllowedServiceLevel() {
        assertEquals(Optional.of(new BigDecimal("95")), client("key", "ECO").cheapestRate(PARKTOWN, 6, new BigDecimal("300")));
        assertEquals(Optional.of(new BigDecimal("86.25")), client("key", "ECO, LOF").cheapestRate(PARKTOWN, 6, new BigDecimal("300")));
        assertEquals("Bearer key", auth.get());
    }

    @Test
    void sendsTheAreaOnlyAndOneParcelPerFullBox() throws Exception {
        client("key", "ECO").cheapestRate(PARKTOWN, 50, new BigDecimal("640.00"));

        JsonNode sent = new ObjectMapper().readTree(body.get());
        JsonNode delivery = sent.path("delivery_address");
        assertFalse(delivery.has("street_address"));
        assertEquals("Parktown", delivery.path("local_area").asText());
        assertEquals("Gauteng", delivery.path("zone").asText());
        assertEquals("2193", delivery.path("code").asText());
        assertEquals("Western Cape", sent.path("collection_address").path("zone").asText());
        assertEquals(3, sent.path("parcels").size());
        assertEquals(1.69, sent.path("parcels").get(0).path("submitted_weight_kg").asDouble(), 0.001);
        assertEquals(0.37, sent.path("parcels").get(2).path("submitted_weight_kg").asDouble(), 0.001);
        assertEquals(640.0, sent.path("declared_value").asDouble(), 0.001);
    }

    @Test
    void anyFailureMeansNoLiveRate() {
        assertTrue(client("", "ECO").cheapestRate(PARKTOWN, 6, BigDecimal.TEN).isEmpty());
        assertTrue(client("key", "ECO").cheapestRate(null, 6, BigDecimal.TEN).isEmpty());
        answer = "{\"message\":\"could not geocode delivery address\",\"rates\":[]}";
        assertTrue(client("key", "ECO").cheapestRate(PARKTOWN, 6, BigDecimal.TEN).isEmpty());
        status = 401;
        answer = "Authentication failed";
        assertTrue(client("key", "ECO").cheapestRate(PARKTOWN, 6, BigDecimal.TEN).isEmpty());
        status = 200;
        answer = "not json";
        assertTrue(client("key", "ECO").cheapestRate(PARKTOWN, 6, BigDecimal.TEN).isEmpty());
    }

    @Test
    void anIncompleteAreaIsNoArea() {
        assertEquals(null, DeliveryArea.of("Parktown", "Johannesburg", null, "2193"));
        assertEquals(null, DeliveryArea.of("Parktown", " ", Province.GAUTENG, "2193"));
        assertEquals(null, DeliveryArea.of("Parktown", "Johannesburg", Province.GAUTENG, "219"));
        assertEquals("Parktown", DeliveryArea.of(" Parktown ", "Johannesburg", Province.GAUTENG, "2193").suburb());
    }
}
