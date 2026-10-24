package za.ac.cput.guiltfreecookie.dto.discount;

import za.ac.cput.guiltfreecookie.domain.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DiscountRequest(
        String code,
        DiscountType type,
        BigDecimal value,
        BigDecimal minSubtotal,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Integer maxUses,
        Boolean onePerEmail,
        String restrictedToEmail,
        String description,
        Boolean active) {
}
