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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// The Courier Guy's ShipLogic API: quotes, bookings, waybills and tracking. The API key decides sandbox or live;
// both use the same host. Switched off while the key is blank.
@Component
public class ShipLogicClient {

    private static final Logger log = LoggerFactory.getLogger(ShipLogicClient.class);
    // Checkout waits on quotes, so they give up quickly. An admin booking can wait longer.
    static final Duration TIMEOUT = Duration.ofSeconds(3);
    static final Duration BOOKING_TIMEOUT = Duration.ofSeconds(20);
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

    public record Collection(String company, String streetAddress, String suburb, String city, Province province, String postalCode,
                             String contactName, String contactPhone, String contactEmail) {
    }

    // One box: its outside size, its empty weight, and how many cookies fit.
    public record Box(int lengthCm, int widthCm, int heightCm, int emptyGrams, int gramsPerCookie, int cookiesPerBox) {
    }

    // Who receives a booked parcel. streetAddress includes any unit or complex.
    public record Recipient(String name, String mobile, String email, String streetAddress, DeliveryArea area, String instructions) {
    }

    public record Booking(String trackingReference, BigDecimal rate) {
    }

    private final String apiKey;
    private final String baseUrl;
    private final String trackingUrl;
    private final Collection collection;
    private final Box box;
    private final Set<String> serviceLevels;
    private final HttpClient client;

    @Autowired
    public ShipLogicClient(
            @Value("${app.shiplogic.api-key:}") String apiKey,
            @Value("${app.shiplogic.base-url:https://api.shiplogic.com}") String baseUrl,
            @Value("${app.shiplogic.service-levels:ECO}") String serviceLevels,
            @Value("${app.shiplogic.tracking-url:https://portal.thecourierguy.co.za/track?ref={ref}}") String trackingUrl,
            @Value("${app.shiplogic.collection.company:Guilt Free Cookie}") String company,
            @Value("${app.shiplogic.collection.street-address:27 Spine Road}") String streetAddress,
            @Value("${app.shiplogic.collection.suburb:Strandfontein}") String suburb,
            @Value("${app.shiplogic.collection.city:Cape Town}") String city,
            @Value("${app.shiplogic.collection.province:WESTERN_CAPE}") Province province,
            @Value("${app.shiplogic.collection.postal-code:7798}") String postalCode,
            @Value("${app.shiplogic.collection.contact-name:Guilt Free Cookie}") String contactName,
            @Value("${app.shiplogic.collection.contact-phone:0215550123}") String contactPhone,
            @Value("${app.shiplogic.collection.contact-email:${app.site.support-email:hello@example.test}}") String contactEmail,
            @Value("${app.shiplogic.box.length-cm:32}") int lengthCm,
            @Value("${app.shiplogic.box.width-cm:24}") int widthCm,
            @Value("${app.shiplogic.box.height-cm:10}") int heightCm,
            @Value("${app.shiplogic.box.empty-grams:280}") int emptyGrams,
            @Value("${app.shiplogic.box.grams-per-cookie:45}") int gramsPerCookie,
            @Value("${app.shiplogic.box.cookies-per-box:24}") int cookiesPerBox) {
        this(apiKey, baseUrl, serviceLevels, trackingUrl,
                new Collection(company, streetAddress, suburb, city, province, postalCode, contactName, contactPhone, contactEmail),
                new Box(lengthCm, widthCm, heightCm, emptyGrams, gramsPerCookie, cookiesPerBox),
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    ShipLogicClient(String apiKey, String baseUrl, String serviceLevels, String trackingUrl, Collection collection, Box box,
                    HttpClient client) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.trackingUrl = trackingUrl;
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
        try {
            return cheapest(send(post("/v2/rates", ratesBody(area, null, cookieCount, declaredValue), TIMEOUT)))
                    .map(rate -> rate.path("rate").decimalValue());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("ShipLogic rates failed: {}", e.toString());
            return Optional.empty();
        }
    }

    // Books a collection from the bakery with the cheapest allowed service, quoted again just before booking.
    public Booking book(String orderNumber, Recipient to, int cookieCount, BigDecimal declaredValue) {
        if (!enabled()) {
            throw new ShipLogicException("Courier booking isn't set up");
        }
        try {
            ObjectNode body = ratesBody(to.area(), to.streetAddress(), cookieCount, declaredValue);
            JsonNode service = cheapest(send(post("/v2/rates", body, BOOKING_TIMEOUT)))
                    .orElseThrow(() -> new ShipLogicException("The Courier Guy has no service to this address"));
            body.put("service_level_id", service.path("service_level").path("id").asLong());
            body.putObject("collection_contact")
                    .put("name", collection.contactName())
                    .put("mobile_number", collection.contactPhone())
                    .put("email", collection.contactEmail());
            body.putObject("delivery_contact")
                    .put("name", to.name())
                    .put("mobile_number", to.mobile())
                    .put("email", to.email());
            body.put("customer_reference", orderNumber);
            if (to.instructions() != null && !to.instructions().isBlank()) {
                body.put("special_instructions_delivery", to.instructions());
            }
            JsonNode shipment = send(post("/v2/shipments", body, BOOKING_TIMEOUT));
            String reference = shipment.path("short_tracking_reference").asText("");
            if (reference.isEmpty()) {
                throw new ShipLogicException("The Courier Guy didn't return a tracking number");
            }
            return new Booking(reference, shipment.path("rate").decimalValue());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ShipLogicException("Booking was interrupted");
        } catch (ShipLogicException e) {
            throw e;
        } catch (Exception e) {
            log.warn("ShipLogic booking failed: {}", e.toString());
            throw new ShipLogicException("The Courier Guy couldn't be reached");
        }
    }

    // A short-lived link to the printable waybill PDF.
    public String waybillUrl(String trackingReference) {
        try {
            JsonNode found = send(get("/v2/shipments?tracking_reference=" + encode(trackingReference), BOOKING_TIMEOUT));
            JsonNode shipment = found.path("shipments").path(0);
            if (shipment.isMissingNode()) {
                throw new ShipLogicException("The Courier Guy has no shipment " + trackingReference);
            }
            String url = send(get("/v2/shipments/label?id=" + shipment.path("id").asLong(), BOOKING_TIMEOUT)).path("url").asText("");
            if (!url.startsWith("https://")) {
                throw new ShipLogicException("The Courier Guy didn't return a waybill");
            }
            return url;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ShipLogicException("Waybill download was interrupted");
        } catch (ShipLogicException e) {
            throw e;
        } catch (Exception e) {
            log.warn("ShipLogic waybill failed: {}", e.toString());
            throw new ShipLogicException("The Courier Guy couldn't be reached");
        }
    }

    // The courier's latest status for a parcel, e.g. "in-transit" or "delivered". Empty if it can't be found right now.
    public Optional<String> status(String trackingReference) {
        if (!enabled()) {
            return Optional.empty();
        }
        try {
            JsonNode found = send(get("/v2/tracking/shipments?tracking_reference=" + encode(trackingReference), BOOKING_TIMEOUT));
            String status = found.path("shipments").path(0).path("status").asText("");
            return status.isEmpty() ? Optional.empty() : Optional.of(status);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("ShipLogic tracking for {} failed: {}", trackingReference, e.toString());
            return Optional.empty();
        }
    }

    // Whether ShipLogic answers and accepts the key.
    public boolean reachable() {
        try {
            send(get("/v2/accounts/webhooks", TIMEOUT));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public String trackingUrl(String trackingReference) {
        return trackingUrl.replace("{ref}", encode(trackingReference));
    }

    Optional<JsonNode> cheapest(JsonNode body) {
        JsonNode best = null;
        for (JsonNode rate : body.path("rates")) {
            String code = rate.path("service_level").path("code").asText("");
            if (!serviceLevels.contains(code) || !rate.path("rate").isNumber()) {
                continue;
            }
            if (best == null || rate.path("rate").decimalValue().compareTo(best.path("rate").decimalValue()) < 0) {
                best = rate;
            }
        }
        return Optional.ofNullable(best);
    }

    ObjectNode ratesBody(DeliveryArea area, String streetAddress, int cookieCount, BigDecimal declaredValue) {
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
        ObjectNode to = body.putObject("delivery_address")
                .put("type", "residential")
                .put("local_area", area.suburb())
                .put("city", area.city())
                .put("zone", ZONES.get(area.province()))
                .put("country", "ZA")
                .put("code", area.postalCode());
        if (streetAddress != null) {
            to.put("street_address", streetAddress);
        }
        ArrayNode parcels = body.putArray("parcels");
        int remaining = Math.max(1, cookieCount);
        while (remaining > 0) {
            int inBox = Math.min(remaining, box.cookiesPerBox());
            double kg = (box.emptyGrams() + inBox * box.gramsPerCookie()) / 1000.0;
            parcels.addObject()
                    .put("submitted_length_cm", box.lengthCm())
                    .put("submitted_width_cm", box.widthCm())
                    .put("submitted_height_cm", box.heightCm())
                    .put("submitted_weight_kg", kg)
                    .put("parcel_description", inBox + " cookies");
            remaining -= inBox;
        }
        body.put("declared_value", declaredValue);
        return body;
    }

    private HttpRequest post(String path, JsonNode body, Duration timeout) {
        return request(path, timeout).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
    }

    private HttpRequest get(String path, Duration timeout) {
        return request(path, timeout).GET().build();
    }

    private HttpRequest.Builder request(String path, Duration timeout) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(timeout).header("Authorization", "Bearer " + apiKey);
    }

    // ShipLogic explains a refused request in plain text, e.g. "invalid delivery address". That goes to the admin.
    private JsonNode send(HttpRequest request) throws Exception {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400 && response.statusCode() < 500 && !response.body().isBlank()) {
            throw new ShipLogicException("The Courier Guy said: " + response.body().strip().lines().findFirst().orElse(""));
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException("ShipLogic answered " + response.statusCode());
        }
        return JSON.readTree(response.body());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
