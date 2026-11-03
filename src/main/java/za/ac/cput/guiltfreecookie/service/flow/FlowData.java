package za.ac.cput.guiltfreecookie.service.flow;

import java.util.List;
import java.util.Map;

// Everything a flow email shows, gathered by FlowDataLoader (or invented for the admin previews).
public record FlowData(
        List<Map<String, Object>> products,
        List<Map<String, Object>> items,
        List<Map<String, Object>> reviews,
        List<String> deliveryFacts,
        String discountCode,
        String discountExpiry,
        int discountPercent,
        String cookieName,
        String primaryUrl,
        int replenishmentDays) {
}
