package za.ac.cput.guiltfreecookie.dto.catalog;

import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.domain.StockStatus;

import java.math.BigDecimal;

// "name" is Cookie.description: that field holds the display name in this codebase.
public record CookieSummary(
        String cookieId,
        String slug,
        String name,
        CookieCategory category,
        String image,
        BigDecimal fromPrice,
        boolean featured,
        StockStatus stockStatus,
        Double averageRating,
        int reviewCount) {
}
