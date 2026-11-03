package za.ac.cput.guiltfreecookie.dto.engagement;

import java.util.Map;

// histogram always has all five keys (5..1), zero when empty.
public record ReviewSummary(Double averageRating, long reviewCount, Map<Integer, Long> histogram) {
}
