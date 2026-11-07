package za.ac.cput.guiltfreecookie.service.address;

import za.ac.cput.guiltfreecookie.util.ShopMetrics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import za.ac.cput.guiltfreecookie.domain.Province;
import za.ac.cput.guiltfreecookie.dto.address.AddressSearchResult;
import za.ac.cput.guiltfreecookie.dto.address.AddressSuggestion;
import za.ac.cput.guiltfreecookie.dto.order.AddressRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// Address search for checkout through Google Places (New). The key stays on the server.
// Switched off while the key is blank. Only South African addresses are suggested.
@Service
public class AddressSearchService {

    private static final Logger log = LoggerFactory.getLogger(AddressSearchService.class);
    static final Duration TIMEOUT = Duration.ofSeconds(3);
    static final int MIN_QUERY_LENGTH = 3;
    static final int MAX_QUERY_LENGTH = 120;
    static final int MAX_SUGGESTIONS = 5;
    private static final Pattern PLACE_ID = Pattern.compile("[A-Za-z0-9_-]{1,300}");
    // Google groups a search's keystrokes and the chosen place into one billed session.
    private static final Pattern SESSION = Pattern.compile("[A-Za-z0-9-]{1,64}");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient client;

    @Autowired
    public AddressSearchService(@Value("${app.address-search.google-api-key:}") String apiKey,
                                @Value("${app.address-search.base-url:https://places.googleapis.com}") String baseUrl) {
        this(apiKey, baseUrl, HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    AddressSearchService(String apiKey, String baseUrl, HttpClient client) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl;
        this.client = client;
    }

    public boolean enabled() {
        return !apiKey.isEmpty();
    }

    public AddressSearchResult suggest(String query, String session) {
        String cleaned = Helper.clean(query);
        if (!enabled() || cleaned == null || cleaned.length() < MIN_QUERY_LENGTH) {
            return new AddressSearchResult(enabled(), false, List.of());
        }
        if (cleaned.length() > MAX_QUERY_LENGTH) {
            cleaned = cleaned.substring(0, MAX_QUERY_LENGTH);
        }
        ObjectNode body = JSON.createObjectNode()
                .put("input", cleaned)
                .put("languageCode", "en");
        body.putArray("includedRegionCodes").add("za");
        if (validSession(session) != null) {
            body.put("sessionToken", session);
        }
        HttpRequest request = request(URI.create(baseUrl + "/v1/places:autocomplete"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        try {
            JsonNode answer = send(request);
            List<AddressSuggestion> suggestions = new ArrayList<>();
            for (JsonNode item : answer.path("suggestions")) {
                JsonNode place = item.path("placePrediction");
                if (place.isMissingNode() || suggestions.size() >= MAX_SUGGESTIONS) {
                    continue;
                }
                String main = place.path("structuredFormat").path("mainText").path("text").asText("");
                String secondary = place.path("structuredFormat").path("secondaryText").path("text").asText("");
                if (main.isEmpty()) {
                    main = place.path("text").path("text").asText("");
                }
                suggestions.add(new AddressSuggestion(place.path("placeId").asText(), main, secondary));
            }
            ShopMetrics.addressSearch("ok");
            return new AddressSearchResult(true, false, suggestions);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Address suggestions failed: {}", e.toString());
            ShopMetrics.addressSearch("failed");
            return new AddressSearchResult(true, true, List.of());
        }
    }

    // The chosen suggestion as checkout's address fields. Parts Google doesn't know are left blank for the customer.
    public AddressRequest place(String placeId, String session) {
        if (!enabled()) {
            throw new NotFoundException("Address search isn't switched on");
        }
        if (placeId == null || !PLACE_ID.matcher(placeId).matches()) {
            throw new BadRequestException("Choose an address from the list");
        }
        String query = "?languageCode=en" + (validSession(session) == null ? ""
                : "&sessionToken=" + URLEncoder.encode(session, StandardCharsets.UTF_8));
        HttpRequest request = request(URI.create(baseUrl + "/v1/places/" + placeId + query))
                .header("X-Goog-FieldMask", "addressComponents")
                .GET()
                .build();
        try {
            return toAddress(send(request).path("addressComponents"));
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Address lookup failed: {}", e.toString());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    static AddressRequest toAddress(JsonNode components) {
        String subpremise = component(components, "subpremise");
        String premise = component(components, "premise");
        String complex = String.join(", ", List.of(subpremise, premise).stream().filter(part -> !part.isEmpty()).toList());
        String locality = component(components, "locality");
        String suburb = first(component(components, "sublocality_level_1"), component(components, "sublocality"),
                component(components, "neighborhood"), locality);
        String city = first(locality, component(components, "postal_town"), component(components, "administrative_area_level_2"));
        return new AddressRequest(
                component(components, "street_number"),
                component(components, "route"),
                complex,
                suburb,
                city,
                province(component(components, "administrative_area_level_1")),
                component(components, "postal_code"));
    }

    // "KwaZulu-Natal" → KWAZULU_NATAL. Null when it isn't one of the nine provinces.
    static Province province(String name) {
        String normalised = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]+", "_").replaceAll("^_|_$", "");
        for (Province province : Province.values()) {
            if (province.name().equals(normalised)) {
                return province;
            }
        }
        return null;
    }

    private static String component(JsonNode components, String type) {
        for (JsonNode component : components) {
            for (JsonNode kind : component.path("types")) {
                if (type.equals(kind.asText())) {
                    return component.path("longText").asText("");
                }
            }
        }
        return "";
    }

    private static String first(String... values) {
        for (String value : values) {
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static String validSession(String session) {
        return session != null && SESSION.matcher(session).matches() ? session : null;
    }

    private HttpRequest.Builder request(URI uri) {
        return HttpRequest.newBuilder(uri).timeout(TIMEOUT).header("X-Goog-Api-Key", apiKey);
    }

    private JsonNode send(HttpRequest request) throws Exception {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Google Places answered " + response.statusCode());
        }
        return JSON.readTree(response.body());
    }
}
