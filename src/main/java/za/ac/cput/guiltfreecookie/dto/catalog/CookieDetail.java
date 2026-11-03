package za.ac.cput.guiltfreecookie.dto.catalog;

import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.domain.StockStatus;

import java.math.BigDecimal;
import java.util.List;

public record CookieDetail(
        String cookieId,
        String slug,
        String name,
        CookieCategory category,
        String longDescription,
        String ingredients,
        String allergies,
        String image,
        List<ImageView> images,
        List<VariantView> variants,
        BigDecimal fromPrice,
        boolean featured,
        StockStatus stockStatus,
        Integer lowStockRemaining,
        Nutrition nutrition,
        Double averageRating,
        int reviewCount,
        List<CookieSummary> related) {
}
