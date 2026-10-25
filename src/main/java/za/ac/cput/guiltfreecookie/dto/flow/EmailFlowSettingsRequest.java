package za.ac.cput.guiltfreecookie.dto.flow;

public record EmailFlowSettingsRequest(
        Boolean welcomeEnabled,
        Boolean browseEnabled,
        Boolean cartEnabled,
        Boolean postPurchaseEnabled,
        Boolean replenishmentEnabled,
        Boolean winbackEnabled,
        Boolean sunsetEnabled,
        Integer welcomeDiscountPercent,
        Integer welcomeCodeDays,
        Integer cartDiscountPercent,
        Integer winbackDiscountPercent,
        Integer replenishmentDays,
        Integer winbackDays,
        Integer sunsetDays) {
}
