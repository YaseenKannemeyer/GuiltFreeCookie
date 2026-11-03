package za.ac.cput.guiltfreecookie.dto.engagement;

import za.ac.cput.guiltfreecookie.dto.catalog.PageResponse;

public record ReviewPage(ReviewSummary summary, PageResponse<ReviewView> page) {
}
