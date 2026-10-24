package za.ac.cput.guiltfreecookie.dto.engagement;

public record ReviewSummary(
    Double averageRating,
    long reviewCount,
    java.util.Map<Integer, Long> histogram
) {}
