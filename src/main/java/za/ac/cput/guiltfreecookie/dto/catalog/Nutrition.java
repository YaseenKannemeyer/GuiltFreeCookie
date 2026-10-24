package za.ac.cput.guiltfreecookie.dto.catalog;

public record Nutrition(
        Integer weightGrams,
        Integer calories,
        Double sugarGrams,
        Double proteinGrams,
        Double carbsGrams,
        Double fatGrams) {
}
