package za.ac.cput.guiltfreecookie.dto.engagement;

public record ReviewPage(
    ReviewSummary summary,
    za.ac.cput.guiltfreecookie.dto.catalog.PageResponse<ReviewView> page
) {}
