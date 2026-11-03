package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "discount_codes")
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "discount_id")
    private String discountId;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType type;

    // Percent (e.g. 10) or rand amount; unused for FREE_SHIPPING.
    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "min_subtotal", precision = 10, scale = 2)
    private BigDecimal minSubtotal;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    // Null = unlimited. 1 = single use (e.g. a personal welcome code).
    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count")
    private int usedCount;

    @Column(name = "one_per_email")
    private boolean onePerEmail;

    // Personal codes only work for this email.
    @Column(name = "restricted_to_email", length = 254)
    private String restrictedToEmail;

    @Column(length = 150)
    private String description;

    private boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected DiscountCode() {
    }

    private DiscountCode(Builder builder) {
        this.discountId = builder.discountId;
        this.code = builder.code;
        this.type = builder.type;
        this.value = builder.value;
        this.minSubtotal = builder.minSubtotal;
        this.startsAt = builder.startsAt;
        this.endsAt = builder.endsAt;
        this.maxUses = builder.maxUses;
        this.usedCount = builder.usedCount;
        this.onePerEmail = builder.onePerEmail;
        this.restrictedToEmail = builder.restrictedToEmail;
        this.description = builder.description;
        this.active = builder.active;
        this.createdAt = builder.createdAt;
    }

    public String getDiscountId() {
        return discountId;
    }

    public String getCode() {
        return code;
    }

    public DiscountType getType() {
        return type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public BigDecimal getMinSubtotal() {
        return minSubtotal;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public boolean isOnePerEmail() {
        return onePerEmail;
    }

    public String getRestrictedToEmail() {
        return restrictedToEmail;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isUsedUp() {
        return maxUses != null && usedCount >= maxUses;
    }

    public static class Builder {
        private String discountId;
        private String code;
        private DiscountType type;
        private BigDecimal value;
        private BigDecimal minSubtotal;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
        private Integer maxUses;
        private int usedCount;
        private boolean onePerEmail;
        private String restrictedToEmail;
        private String description;
        private boolean active = true;
        private LocalDateTime createdAt;

        public Builder setDiscountId(String discountId) {
            this.discountId = discountId;
            return this;
        }

        public Builder setCode(String code) {
            this.code = code;
            return this;
        }

        public Builder setType(DiscountType type) {
            this.type = type;
            return this;
        }

        public Builder setValue(BigDecimal value) {
            this.value = value;
            return this;
        }

        public Builder setMinSubtotal(BigDecimal minSubtotal) {
            this.minSubtotal = minSubtotal;
            return this;
        }

        public Builder setStartsAt(LocalDateTime startsAt) {
            this.startsAt = startsAt;
            return this;
        }

        public Builder setEndsAt(LocalDateTime endsAt) {
            this.endsAt = endsAt;
            return this;
        }

        public Builder setMaxUses(Integer maxUses) {
            this.maxUses = maxUses;
            return this;
        }

        public Builder setUsedCount(int usedCount) {
            this.usedCount = usedCount;
            return this;
        }

        public Builder setOnePerEmail(boolean onePerEmail) {
            this.onePerEmail = onePerEmail;
            return this;
        }

        public Builder setRestrictedToEmail(String restrictedToEmail) {
            this.restrictedToEmail = restrictedToEmail;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setActive(boolean active) {
            this.active = active;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder copy(DiscountCode discount) {
            this.discountId = discount.discountId;
            this.code = discount.code;
            this.type = discount.type;
            this.value = discount.value;
            this.minSubtotal = discount.minSubtotal;
            this.startsAt = discount.startsAt;
            this.endsAt = discount.endsAt;
            this.maxUses = discount.maxUses;
            this.usedCount = discount.usedCount;
            this.onePerEmail = discount.onePerEmail;
            this.restrictedToEmail = discount.restrictedToEmail;
            this.description = discount.description;
            this.active = discount.active;
            this.createdAt = discount.createdAt;
            return this;
        }

        public DiscountCode build() {
            return new DiscountCode(this);
        }
    }
}
