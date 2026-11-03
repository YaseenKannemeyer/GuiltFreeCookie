package za.ac.cput.guiltfreecookie.dto.catalog;

public record CookieDetailsRequest(
        String slug,
        Boolean featured,
        Integer lowStockThreshold,
        Integer weightGrams,
        Integer calories,
        Double sugarGrams,
        Double proteinGrams,
        Double carbsGrams,
        Double fatGrams) {
}
