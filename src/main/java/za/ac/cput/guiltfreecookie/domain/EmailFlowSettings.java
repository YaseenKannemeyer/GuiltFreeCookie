package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// One row (settingsId "default"), edited by admins. Seeded from app.flows.* the first time it's read.
@Entity
@Table(name = "email_flow_settings")
public class EmailFlowSettings {

    public static final String DEFAULT_ID = "default";

    @Id
    @Column(name = "settings_id", length = 20)
    private String settingsId;

    @Column(name = "welcome_enabled")
    private boolean welcomeEnabled;

    @Column(name = "browse_enabled")
    private boolean browseEnabled;

    @Column(name = "cart_enabled")
    private boolean cartEnabled;

    @Column(name = "post_purchase_enabled")
    private boolean postPurchaseEnabled;

    @Column(name = "replenishment_enabled")
    private boolean replenishmentEnabled;

    @Column(name = "winback_enabled")
    private boolean winbackEnabled;

    @Column(name = "sunset_enabled")
    private boolean sunsetEnabled;

    @Column(name = "welcome_discount_percent")
    private int welcomeDiscountPercent;

    @Column(name = "welcome_code_days")
    private int welcomeCodeDays;

    @Column(name = "cart_discount_percent")
    private int cartDiscountPercent;

    @Column(name = "winback_discount_percent")
    private int winbackDiscountPercent;

    @Column(name = "replenishment_days")
    private int replenishmentDays;

    @Column(name = "winback_days")
    private int winbackDays;

    @Column(name = "sunset_days")
    private int sunsetDays;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected EmailFlowSettings() {
    }

    private EmailFlowSettings(Builder builder) {
        this.settingsId = builder.settingsId;
        this.welcomeEnabled = builder.welcomeEnabled;
        this.browseEnabled = builder.browseEnabled;
        this.cartEnabled = builder.cartEnabled;
        this.postPurchaseEnabled = builder.postPurchaseEnabled;
        this.replenishmentEnabled = builder.replenishmentEnabled;
        this.winbackEnabled = builder.winbackEnabled;
        this.sunsetEnabled = builder.sunsetEnabled;
        this.welcomeDiscountPercent = builder.welcomeDiscountPercent;
        this.welcomeCodeDays = builder.welcomeCodeDays;
        this.cartDiscountPercent = builder.cartDiscountPercent;
        this.winbackDiscountPercent = builder.winbackDiscountPercent;
        this.replenishmentDays = builder.replenishmentDays;
        this.winbackDays = builder.winbackDays;
        this.sunsetDays = builder.sunsetDays;
        this.updatedAt = builder.updatedAt;
    }

    public String getSettingsId() {
        return settingsId;
    }

    public boolean isWelcomeEnabled() {
        return welcomeEnabled;
    }

    public boolean isBrowseEnabled() {
        return browseEnabled;
    }

    public boolean isCartEnabled() {
        return cartEnabled;
    }

    public boolean isPostPurchaseEnabled() {
        return postPurchaseEnabled;
    }

    public boolean isReplenishmentEnabled() {
        return replenishmentEnabled;
    }

    public boolean isWinbackEnabled() {
        return winbackEnabled;
    }

    public boolean isSunsetEnabled() {
        return sunsetEnabled;
    }

    public int getWelcomeDiscountPercent() {
        return welcomeDiscountPercent;
    }

    public int getWelcomeCodeDays() {
        return welcomeCodeDays;
    }

    public int getCartDiscountPercent() {
        return cartDiscountPercent;
    }

    public int getWinbackDiscountPercent() {
        return winbackDiscountPercent;
    }

    public int getReplenishmentDays() {
        return replenishmentDays;
    }

    public int getWinbackDays() {
        return winbackDays;
    }

    public int getSunsetDays() {
        return sunsetDays;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isEnabled(EmailFlow flow) {
        return switch (flow) {
            case WELCOME -> welcomeEnabled;
            case BROWSE_ABANDONMENT -> browseEnabled;
            case CART_ABANDONMENT -> cartEnabled;
            case POST_PURCHASE -> postPurchaseEnabled;
            case REPLENISHMENT -> replenishmentEnabled;
            case WINBACK -> winbackEnabled;
            case SUNSET -> sunsetEnabled;
        };
    }

    public static class Builder {
        private String settingsId;
        private boolean welcomeEnabled;
        private boolean browseEnabled;
        private boolean cartEnabled;
        private boolean postPurchaseEnabled;
        private boolean replenishmentEnabled;
        private boolean winbackEnabled;
        private boolean sunsetEnabled;
        private int welcomeDiscountPercent;
        private int welcomeCodeDays;
        private int cartDiscountPercent;
        private int winbackDiscountPercent;
        private int replenishmentDays;
        private int winbackDays;
        private int sunsetDays;
        private LocalDateTime updatedAt;

        public Builder setSettingsId(String settingsId) {
            this.settingsId = settingsId;
            return this;
        }

        public Builder setWelcomeEnabled(boolean welcomeEnabled) {
            this.welcomeEnabled = welcomeEnabled;
            return this;
        }

        public Builder setBrowseEnabled(boolean browseEnabled) {
            this.browseEnabled = browseEnabled;
            return this;
        }

        public Builder setCartEnabled(boolean cartEnabled) {
            this.cartEnabled = cartEnabled;
            return this;
        }

        public Builder setPostPurchaseEnabled(boolean postPurchaseEnabled) {
            this.postPurchaseEnabled = postPurchaseEnabled;
            return this;
        }

        public Builder setReplenishmentEnabled(boolean replenishmentEnabled) {
            this.replenishmentEnabled = replenishmentEnabled;
            return this;
        }

        public Builder setWinbackEnabled(boolean winbackEnabled) {
            this.winbackEnabled = winbackEnabled;
            return this;
        }

        public Builder setSunsetEnabled(boolean sunsetEnabled) {
            this.sunsetEnabled = sunsetEnabled;
            return this;
        }

        public Builder setWelcomeDiscountPercent(int welcomeDiscountPercent) {
            this.welcomeDiscountPercent = welcomeDiscountPercent;
            return this;
        }

        public Builder setWelcomeCodeDays(int welcomeCodeDays) {
            this.welcomeCodeDays = welcomeCodeDays;
            return this;
        }

        public Builder setCartDiscountPercent(int cartDiscountPercent) {
            this.cartDiscountPercent = cartDiscountPercent;
            return this;
        }

        public Builder setWinbackDiscountPercent(int winbackDiscountPercent) {
            this.winbackDiscountPercent = winbackDiscountPercent;
            return this;
        }

        public Builder setReplenishmentDays(int replenishmentDays) {
            this.replenishmentDays = replenishmentDays;
            return this;
        }

        public Builder setWinbackDays(int winbackDays) {
            this.winbackDays = winbackDays;
            return this;
        }

        public Builder setSunsetDays(int sunsetDays) {
            this.sunsetDays = sunsetDays;
            return this;
        }

        public Builder setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder copy(EmailFlowSettings emailFlowSettings) {
            this.settingsId = emailFlowSettings.settingsId;
            this.welcomeEnabled = emailFlowSettings.welcomeEnabled;
            this.browseEnabled = emailFlowSettings.browseEnabled;
            this.cartEnabled = emailFlowSettings.cartEnabled;
            this.postPurchaseEnabled = emailFlowSettings.postPurchaseEnabled;
            this.replenishmentEnabled = emailFlowSettings.replenishmentEnabled;
            this.winbackEnabled = emailFlowSettings.winbackEnabled;
            this.sunsetEnabled = emailFlowSettings.sunsetEnabled;
            this.welcomeDiscountPercent = emailFlowSettings.welcomeDiscountPercent;
            this.welcomeCodeDays = emailFlowSettings.welcomeCodeDays;
            this.cartDiscountPercent = emailFlowSettings.cartDiscountPercent;
            this.winbackDiscountPercent = emailFlowSettings.winbackDiscountPercent;
            this.replenishmentDays = emailFlowSettings.replenishmentDays;
            this.winbackDays = emailFlowSettings.winbackDays;
            this.sunsetDays = emailFlowSettings.sunsetDays;
            this.updatedAt = emailFlowSettings.updatedAt;
            return this;
        }

        public EmailFlowSettings build() {
            return new EmailFlowSettings(this);
        }
    }
}
