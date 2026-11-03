package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

// A pack size such as "Box of 6". Stock is counted in single cookies, so
// buying one pack uses unitsPerPack cookies.
@Entity
@Table(name = "cookie_variants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cookie_id", "label"}))
public class CookieVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "variant_id")
    private String variantId;

    @Column(name = "cookie_id", nullable = false)
    private String cookieId;

    @Column(nullable = false, length = 50)
    private String label;

    @Column(name = "units_per_pack", nullable = false)
    private int unitsPerPack;

    // Null means "unit price x unitsPerPack", so the Single pack follows Cookie.price.
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private boolean active;

    @Column(name = "sort_order")
    private int sortOrder;

    protected CookieVariant() {
    }

    private CookieVariant(Builder builder) {
        this.variantId = builder.variantId;
        this.cookieId = builder.cookieId;
        this.label = builder.label;
        this.unitsPerPack = builder.unitsPerPack;
        this.price = builder.price;
        this.active = builder.active;
        this.sortOrder = builder.sortOrder;
    }

    public String getVariantId() {
        return variantId;
    }

    public String getCookieId() {
        return cookieId;
    }

    public String getLabel() {
        return label;
    }

    public int getUnitsPerPack() {
        return unitsPerPack;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public BigDecimal effectivePrice(Cookie cookie) {
        if (price != null) {
            return price.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal unitPrice = cookie.getPrice() == null ? BigDecimal.ZERO : BigDecimal.valueOf(cookie.getPrice());
        return unitPrice.multiply(BigDecimal.valueOf(unitsPerPack)).setScale(2, RoundingMode.HALF_UP);
    }

    public static class Builder {
        private String variantId;
        private String cookieId;
        private String label;
        private int unitsPerPack = 1;
        private BigDecimal price;
        private boolean active = true;
        private int sortOrder;

        public Builder setVariantId(String variantId) {
            this.variantId = variantId;
            return this;
        }

        public Builder setCookieId(String cookieId) {
            this.cookieId = cookieId;
            return this;
        }

        public Builder setLabel(String label) {
            this.label = label;
            return this;
        }

        public Builder setUnitsPerPack(int unitsPerPack) {
            this.unitsPerPack = unitsPerPack;
            return this;
        }

        public Builder setPrice(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder setActive(boolean active) {
            this.active = active;
            return this;
        }

        public Builder setSortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder copy(CookieVariant variant) {
            this.variantId = variant.variantId;
            this.cookieId = variant.cookieId;
            this.label = variant.label;
            this.unitsPerPack = variant.unitsPerPack;
            this.price = variant.price;
            this.active = variant.active;
            this.sortOrder = variant.sortOrder;
            return this;
        }

        public CookieVariant build() {
            return new CookieVariant(this);
        }
    }
}
