package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// One row (settingsId "default"), edited by admins. Seeded from app.shipping.* the first time it's read.
@Entity
@Table(name = "shipping_settings")
public class ShippingSettings {

    public static final String DEFAULT_ID = "default";

    @Id
    @Column(name = "settings_id", length = 20)
    private String settingsId;

    @Column(name = "courier_enabled")
    private boolean courierEnabled;

    @Column(name = "courier_rate", precision = 10, scale = 2)
    private BigDecimal courierRate;

    @Column(name = "courier_eta", length = 100)
    private String courierEta;

    @Column(name = "pudo_enabled")
    private boolean pudoEnabled;

    @Column(name = "pudo_rate", precision = 10, scale = 2)
    private BigDecimal pudoRate;

    @Column(name = "pudo_eta", length = 100)
    private String pudoEta;

    @Column(name = "pickup_enabled")
    private boolean pickupEnabled;

    @Column(name = "pickup_address", length = 255)
    private String pickupAddress;

    @Column(name = "pickup_instructions", length = 500)
    private String pickupInstructions;

    @Column(name = "pickup_eta", length = 100)
    private String pickupEta;

    // Courier and PUDO are free at or above this (after discounts). Null switches it off.
    @Column(name = "free_shipping_threshold", precision = 10, scale = 2)
    private BigDecimal freeShippingThreshold;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected ShippingSettings() {
    }

    private ShippingSettings(Builder builder) {
        this.settingsId = builder.settingsId;
        this.courierEnabled = builder.courierEnabled;
        this.courierRate = builder.courierRate;
        this.courierEta = builder.courierEta;
        this.pudoEnabled = builder.pudoEnabled;
        this.pudoRate = builder.pudoRate;
        this.pudoEta = builder.pudoEta;
        this.pickupEnabled = builder.pickupEnabled;
        this.pickupAddress = builder.pickupAddress;
        this.pickupInstructions = builder.pickupInstructions;
        this.pickupEta = builder.pickupEta;
        this.freeShippingThreshold = builder.freeShippingThreshold;
        this.updatedAt = builder.updatedAt;
    }

    public String getSettingsId() {
        return settingsId;
    }

    public boolean isCourierEnabled() {
        return courierEnabled;
    }

    public BigDecimal getCourierRate() {
        return courierRate;
    }

    public String getCourierEta() {
        return courierEta;
    }

    public boolean isPudoEnabled() {
        return pudoEnabled;
    }

    public BigDecimal getPudoRate() {
        return pudoRate;
    }

    public String getPudoEta() {
        return pudoEta;
    }

    public boolean isPickupEnabled() {
        return pickupEnabled;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public String getPickupInstructions() {
        return pickupInstructions;
    }

    public String getPickupEta() {
        return pickupEta;
    }

    public BigDecimal getFreeShippingThreshold() {
        return freeShippingThreshold;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static class Builder {
        private String settingsId;
        private boolean courierEnabled;
        private BigDecimal courierRate;
        private String courierEta;
        private boolean pudoEnabled;
        private BigDecimal pudoRate;
        private String pudoEta;
        private boolean pickupEnabled;
        private String pickupAddress;
        private String pickupInstructions;
        private String pickupEta;
        private BigDecimal freeShippingThreshold;
        private LocalDateTime updatedAt;

        public Builder setSettingsId(String settingsId) {
            this.settingsId = settingsId;
            return this;
        }

        public Builder setCourierEnabled(boolean courierEnabled) {
            this.courierEnabled = courierEnabled;
            return this;
        }

        public Builder setCourierRate(BigDecimal courierRate) {
            this.courierRate = courierRate;
            return this;
        }

        public Builder setCourierEta(String courierEta) {
            this.courierEta = courierEta;
            return this;
        }

        public Builder setPudoEnabled(boolean pudoEnabled) {
            this.pudoEnabled = pudoEnabled;
            return this;
        }

        public Builder setPudoRate(BigDecimal pudoRate) {
            this.pudoRate = pudoRate;
            return this;
        }

        public Builder setPudoEta(String pudoEta) {
            this.pudoEta = pudoEta;
            return this;
        }

        public Builder setPickupEnabled(boolean pickupEnabled) {
            this.pickupEnabled = pickupEnabled;
            return this;
        }

        public Builder setPickupAddress(String pickupAddress) {
            this.pickupAddress = pickupAddress;
            return this;
        }

        public Builder setPickupInstructions(String pickupInstructions) {
            this.pickupInstructions = pickupInstructions;
            return this;
        }

        public Builder setPickupEta(String pickupEta) {
            this.pickupEta = pickupEta;
            return this;
        }

        public Builder setFreeShippingThreshold(BigDecimal freeShippingThreshold) {
            this.freeShippingThreshold = freeShippingThreshold;
            return this;
        }

        public Builder setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder copy(ShippingSettings settings) {
            this.settingsId = settings.settingsId;
            this.courierEnabled = settings.courierEnabled;
            this.courierRate = settings.courierRate;
            this.courierEta = settings.courierEta;
            this.pudoEnabled = settings.pudoEnabled;
            this.pudoRate = settings.pudoRate;
            this.pudoEta = settings.pudoEta;
            this.pickupEnabled = settings.pickupEnabled;
            this.pickupAddress = settings.pickupAddress;
            this.pickupInstructions = settings.pickupInstructions;
            this.pickupEta = settings.pickupEta;
            this.freeShippingThreshold = settings.freeShippingThreshold;
            this.updatedAt = settings.updatedAt;
            return this;
        }

        public ShippingSettings build() {
            return new ShippingSettings(this);
        }
    }
}
