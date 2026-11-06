package za.ac.cput.guiltfreecookie.service.shipping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import za.ac.cput.guiltfreecookie.domain.Province;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// The Courier Guy's ShipLogic API. The API key decides sandbox or live; both use the same host.
// Switched off while the key is blank.
@Component
public class ShipLogicClient {

    private static final Logger log = LoggerFactory.getLogger(ShipLogicClient.class);
    static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<Province, String> ZONES = new EnumMap<>(Map.of(
            Province.EASTERN_CAPE, "Eastern Cape",
            Province.FREE_STATE, "Free State",
            Province.GAUTENG, "Gauteng",
            Province.KWAZULU_NATAL, "KwaZulu-Natal",
            Province.LIMPOPO, "Limpopo",
            Province.MPUMALANGA, "Mpumalanga",
            Province.NORTH_WEST, "North West",
            Province.NORTHERN_CAPE, "Northern Cape",
            Province.WESTERN_CAPE, "Western Cape"));

    public record Collection(String company, String streetAddress, String suburb, String city, Province province, String postalCode) {
    }

    // One box: its outside size, its empty weight, and how many cookies fit.
    public record Box(int lengthCm, int widthCm, int heightCm, int emptyGrams, int gramsPerCookie, int cookiesPerBox) {
    }

    private final String apiKey;
    private final URI ratesUrl;
    private final Collection collection;
    private final Box box;
    private final Set<String> serviceLevels;
    private final HttpClient client;

    @Autowired
    public ShipLogicClient(
            @Value("${app.shiplogic.api-key:}") String apiKey,
            @Value("${app.shiplogic.base-url:https://api.shiplogic.com}") String baseUrl,
            @Value("${app.shiplogic.service-levels:ECO}") String serviceLevels,
            @Value("${app.shiplogic.collection.company:Guilt Free Cookie}") String company,
            @Value("${app.shiplogic.collection.street-address:27 Spine Road}") String streetAddress,
            @Value("${app.shiplogic.collection.suburb:Strandfontein}") String suburb,
            @Value("${app.shiplogic.collection.city:Cape Town}") String city,
            @Value("${app.shiplogic.collection.province:WESTERN_CAPE}") Province province,
            @Value("${app.shiplogic.collection.postal-code:7798}") String postalCode,
            @Value("${app.shiplogic.box.length-cm:32}") int lengthCm,
            @Value("${app.shiplogic.box.width-cm:24}") int widthCm,
            @Value("${app.shiplogic.box.height-cm:10}") int heightCm,
            @Value("${app.shiplogic.box.empty-grams:280}") int emptyGrams,
            @Value("${app.shiplogic.box.grams-per-cookie:45}") int gramsPerCookie,
            @Value("${app.shiplogic.box.cookies-per-box:24}") int cookiesPerBox) {
        this(apiKey, baseUrl, serviceLevels,
                new Collection(company, streetAddress, suburb, city, province, postalCode),
                new Box(lengthCm, widthCm, heightCm, emptyGrams, gramsPerCookie, cookiesPerBox),
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    ShipLogicClient(String apiKey, String baseUrl, String serviceLevels, Collection collection, Box box, HttpClient client) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.ratesUrl = URI.create(baseUrl.replaceAll("/+$", "") + "/v2/rates");
        this.serviceLevels = Arrays.stream(serviceLevels.split(","))
                .map(String::trim).filter(code -> !code.isEmpty()).collect(Collectors.toUnmodifiableSet());
        this.collection = collection;
        this.box = box;
        this.client = client;
    }

    public boolean enabled() {
        return !apiKey.isEmpty();
    }

    // The cheapest allowed service's price, including VAT. Empty when switched off, slow, failing or the area isn't served.
    public Optional<BigDecimal> cheapestRate(DeliveryArea area, int cookieCount, BigDecimal declaredValue) {
        if (!enabled() || area == null) {
            return Optional.empty();
        }
        HttpRequest request = HttpRequest.newBuilder(ratesUrl)
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(ratesBody(area, cookieCount, declaredValue)))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("ShipLogic rates answered {}", response.statusCode());
                return Optional.empty();
            }
            return cheapest(JSON.readTree(response.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("ShipLogic rates failed: {}", e.toString());
            return Optional.empty();
        }
    }

    Optional<BigDecimal> cheapest(JsonNode body) {
        Optional<BigDecimal> best = Optional.empty();
        for (JsonNode rate : body.path("rates")) {
            String code = rate.path("service_level").path("code").asText("");
            if (!serviceLevels.contains(code) || !rate.path("rate").isNumber()) {
                continue;
            }
            BigDecimal price = rate.path("rate").decimalValue();
            if (best.isEmpty() || price.compareTo(best.get()) < 0) {
                best = Optional.of(price);
            }
        }
        return best;
    }

    String ratesBody(DeliveryArea area, int cookieCount, BigDecimal declaredValue) {
        ObjectNode body = JSON.createObjectNode();
        ObjectNode from = body.putObject("collection_address")
                .put("type", "business")
                .put("company", collection.company())
                .put("local_area", collection.suburb())
                .put("city", collection.city())
                .put("zone", ZONES.get(collection.province()))
                .put("country", "ZA")
                .put("code", collection.postalCode());
        if (!collection.streetAddress().isBlank()) {
            from.put("street_address", collection.streetAddress());
        }
        body.putObject("delivery_address")
                .put("type", "residential")
                .put("local_area", area.suburb())
                .put("city", area.city())
                .put("zone", ZONES.get(area.province()))
                .put("country", "ZA")
                .put("code", area.postalCode());
        ArrayNode parcels = body.putArray("parcels");
        int remaining = Math.max(1, cookieCount);
        while (remaining > 0) {
            int inBox = Math.min(remaining, box.cookiesPerBox());
            double kg = (box.emptyGrams() + inBox * box.gramsPerCookie()) / 1000.0;
            parcels.addObject()
                    .put("submitted_length_cm", box.lengthCm())
                    .put("submitted_width_cm", box.widthCm())
                    .put("submitted_height_cm", box.heightCm())
                    .put("submitted_weight_kg", kg);
            remaining -= inBox;
        }
        body.put("declared_value", declaredValue);
        return body.toString();
    }
}
