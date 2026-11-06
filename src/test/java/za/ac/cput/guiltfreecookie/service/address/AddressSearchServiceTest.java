package za.ac.cput.guiltfreecookie.service.address;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import za.ac.cput.guiltfreecookie.domain.Province;
import za.ac.cput.guiltfreecookie.dto.address.AddressSearchResult;
import za.ac.cput.guiltfreecookie.dto.order.AddressRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressSearchServiceTest {

    private static final String SUGGESTIONS = """
            {"suggestions":[
              {"placePrediction":{"placeId":"ChIJabc","text":{"text":"10 Jan Smuts Avenue, Parktown, Johannesburg"},
                "structuredFormat":{"mainText":{"text":"10 Jan Smuts Avenue"},"secondaryText":{"text":"Parktown, Johannesburg"}}}},
              {"queryPrediction":{"text":{"text":"jan smuts"}}}
            ]}""";
    private static final String PLACE = """
            {"addressComponents":[
              {"longText":"4","types":["subpremise"]},
              {"longText":"Oak Court","types":["premise"]},
              {"longText":"10","types":["street_number"]},
              {"longText":"Jan Smuts Avenue","types":["route"]},
              {"longText":"Parktown","types":["sublocality_level_1","sublocality","political"]},
              {"longText":"Johannesburg","types":["locality","political"]},
              {"longText":"Gauteng","types":["administrative_area_level_1","political"]},
              {"longText":"South Africa","types":["country","political"]},
              {"longText":"2193","types":["postal_code"]}
            ]}""";

    private HttpServer server;
    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicReference<String> sent = new AtomicReference<>();
    private final AtomicReference<String> placePath = new AtomicReference<>();
    private volatile int status = 200;
    private String base;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/", exchange -> {
            hits.incrementAndGet();
            boolean autocomplete = exchange.getRequestURI().getPath().endsWith("places:autocomplete");
            if (autocomplete) {
                sent.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            } else {
                placePath.set(exchange.getRequestURI().toString() + " " + exchange.getRequestHeaders().getFirst("X-Goog-FieldMask"));
            }
            byte[] bytes = (autocomplete ? SUGGESTIONS : PLACE).getBytes(StandardCharsets.UTF_8);
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

    private AddressSearchService service(String key) {
        return new AddressSearchService(key, base, HttpClient.newHttpClient());
    }

    @Test
    void suggestsSouthAfricanPlacesOnly() throws Exception {
        AddressSearchResult result = service("key").suggest("10 jan smuts", "abc-123");

        assertTrue(result.enabled());
        assertFalse(result.failed());
        assertEquals(1, result.suggestions().size());
        assertEquals("ChIJabc", result.suggestions().getFirst().placeId());
        assertEquals("10 Jan Smuts Avenue", result.suggestions().getFirst().mainText());
        assertEquals("Parktown, Johannesburg", result.suggestions().getFirst().secondaryText());
        var body = new ObjectMapper().readTree(sent.get());
        assertEquals("za", body.path("includedRegionCodes").get(0).asText());
        assertEquals("abc-123", body.path("sessionToken").asText());
    }

    @Test
    void shortQueriesAndAMissingKeyNeverCallGoogle() {
        assertFalse(service("").suggest("10 jan smuts", null).enabled());
        assertTrue(service("key").suggest("10", null).enabled());
        assertEquals(0, hits.get());
        assertThrows(NotFoundException.class, () -> service("").place("ChIJabc", null));
    }

    @Test
    void badSessionsAreDroppedAndBadPlaceIdsRejected() throws Exception {
        service("key").suggest("10 jan smuts", "not a session!");
        assertFalse(new ObjectMapper().readTree(sent.get()).has("sessionToken"));
        assertThrows(BadRequestException.class, () -> service("key").place("../admin", null));
        assertEquals(1, hits.get());
    }

    @Test
    void turnsThePlaceIntoCheckoutAddressFields() {
        AddressRequest address = service("key").place("ChIJabc", "abc-123");

        assertEquals("10", address.streetNumber());
        assertEquals("Jan Smuts Avenue", address.streetName());
        assertEquals("4, Oak Court", address.complex());
        assertEquals("Parktown", address.suburb());
        assertEquals("Johannesburg", address.city());
        assertEquals(Province.GAUTENG, address.province());
        assertEquals("2193", address.postalCode());
        assertTrue(placePath.get().startsWith("/v1/places/ChIJabc?languageCode=en&sessionToken=abc-123 addressComponents"));
    }

    @Test
    void mapsProvinceNames() {
        assertEquals(Province.KWAZULU_NATAL, AddressSearchService.province("KwaZulu-Natal"));
        assertEquals(Province.NORTH_WEST, AddressSearchService.province("North West"));
        assertNull(AddressSearchService.province(""));
        assertNull(AddressSearchService.province("Gaborone"));
    }

    @Test
    void googleFailuresAreReportedNotThrownForSuggestions() {
        status = 500;
        AddressSearchResult result = service("key").suggest("10 jan smuts", null);
        assertTrue(result.failed());
        assertTrue(result.suggestions().isEmpty());
        assertThrows(ResponseStatusException.class, () -> service("key").place("ChIJabc", null));
    }
}
