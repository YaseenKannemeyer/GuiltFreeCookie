package za.ac.cput.guiltfreecookie.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cookies")
public class Cookie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cookie_id")
    private String cookieId;

    @Enumerated(EnumType.STRING)
    private CookieCategory category;

    private String description;

    @Column(name = "long_description", length = 1000)
    private String longDescription;

    private String ingredients;
    private String allergies;
    private Double price;
    private String image;
    private boolean archived;

    // Fields below are managed through /api/v1/admin/cookies, never through the
    // legacy /cookie JSON (READ_ONLY), so the existing admin form can't wipe them.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(unique = true, length = 80)
    private String slug;

    // Individual cookies on hand. Only changed through InventoryService.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean featured;

    // Per cookie.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "weight_grams")
    private Integer weightGrams;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer calories;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "sugar_grams")
    private Double sugarGrams;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "protein_grams")
    private Double proteinGrams;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "carbs_grams")
    private Double carbsGrams;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "fat_grams")
    private Double fatGrams;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected Cookie() {
    }

    private Cookie(Builder builder) {
        this.cookieId = builder.cookieId;
        this.category = builder.category;
        this.description = builder.description;
        this.longDescription = builder.longDescription;
        this.ingredients = builder.ingredients;
        this.allergies = builder.allergies;
        this.price = builder.price;
        this.image = builder.image;
        this.archived = builder.archived;
        this.slug = builder.slug;
        this.stockQuantity = builder.stockQuantity;
        this.lowStockThreshold = builder.lowStockThreshold;
        this.featured = builder.featured;
        this.weightGrams = builder.weightGrams;
        this.calories = builder.calories;
        this.sugarGrams = builder.sugarGrams;
        this.proteinGrams = builder.proteinGrams;
        this.carbsGrams = builder.carbsGrams;
        this.fatGrams = builder.fatGrams;
        this.createdAt = builder.createdAt;
    }

    public String getCookieId() {
        return cookieId;
    }

    public CookieCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public String getIngredients() {
        return ingredients;
    }

    public String getAllergies() {
        return allergies;
    }

    public Double getPrice() {
        return price;
    }

    public String getImage() {
        return image;
    }

    public boolean isArchived() {
        return archived;
    }

    public String getSlug() {
        return slug;
    }

    public int getStockQuantity() {
        return stockQuantity == null ? 0 : stockQuantity;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public boolean isFeatured() {
        return featured;
    }

    public Integer getWeightGrams() {
        return weightGrams;
    }

    public Integer getCalories() {
        return calories;
    }

    public Double getSugarGrams() {
        return sugarGrams;
    }

    public Double getProteinGrams() {
        return proteinGrams;
    }

    public Double getCarbsGrams() {
        return carbsGrams;
    }

    public Double getFatGrams() {
        return fatGrams;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Cookie{" +
                "cookieId='" + cookieId + '\'' +
                ", category=" + category +
                ", description='" + description + '\'' +
                ", longDescription='" + longDescription + '\'' +
                ", ingredients='" + ingredients + '\'' +
                ", allergies='" + allergies + '\'' +
                ", price=" + price +
                ", image='" + image + '\'' +
                ", archived=" + archived +
                '}';
    }

    public static class Builder {
        private String cookieId;
        private CookieCategory category;
        private String description;
        private String longDescription;
        private String ingredients;
        private String allergies;
        private Double price;
        private String image;
        private boolean archived;
        private String slug;
        private Integer stockQuantity;
        private Integer lowStockThreshold;
        private boolean featured;
        private Integer weightGrams;
        private Integer calories;
        private Double sugarGrams;
        private Double proteinGrams;
        private Double carbsGrams;
        private Double fatGrams;
        private LocalDateTime createdAt;

        public Builder setCookieId(String cookieId) {
            this.cookieId = cookieId;
            return this;
        }

        public Builder setCategory(CookieCategory category) {
            this.category = category;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setLongDescription(String longDescription) {
            this.longDescription = longDescription;
            return this;
        }

        public Builder setIngredients(String ingredients) {
            this.ingredients = ingredients;
            return this;
        }

        public Builder setAllergies(String allergies) {
            this.allergies = allergies;
            return this;
        }

        public Builder setPrice(Double price) {
            this.price = price;
            return this;
        }

        public Builder setImage(String image) {
            this.image = image;
            return this;
        }

        public Builder setArchived(boolean archived) {
            this.archived = archived;
            return this;
        }

        public Builder setSlug(String slug) {
            this.slug = slug;
            return this;
        }

        public Builder setStockQuantity(Integer stockQuantity) {
            this.stockQuantity = stockQuantity;
            return this;
        }

        public Builder setLowStockThreshold(Integer lowStockThreshold) {
            this.lowStockThreshold = lowStockThreshold;
            return this;
        }

        public Builder setFeatured(boolean featured) {
            this.featured = featured;
            return this;
        }

        public Builder setWeightGrams(Integer weightGrams) {
            this.weightGrams = weightGrams;
            return this;
        }

        public Builder setCalories(Integer calories) {
            this.calories = calories;
            return this;
        }

        public Builder setSugarGrams(Double sugarGrams) {
            this.sugarGrams = sugarGrams;
            return this;
        }

        public Builder setProteinGrams(Double proteinGrams) {
            this.proteinGrams = proteinGrams;
            return this;
        }

        public Builder setCarbsGrams(Double carbsGrams) {
            this.carbsGrams = carbsGrams;
            return this;
        }

        public Builder setFatGrams(Double fatGrams) {
            this.fatGrams = fatGrams;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder copy(Cookie cookie) {
            this.cookieId = cookie.cookieId;
            this.category = cookie.category;
            this.description = cookie.description;
            this.longDescription = cookie.longDescription;
            this.ingredients = cookie.ingredients;
            this.allergies = cookie.allergies;
            this.price = cookie.price;
            this.image = cookie.image;
            this.archived = cookie.archived;
            this.slug = cookie.slug;
            this.stockQuantity = cookie.stockQuantity;
            this.lowStockThreshold = cookie.lowStockThreshold;
            this.featured = cookie.featured;
            this.weightGrams = cookie.weightGrams;
            this.calories = cookie.calories;
            this.sugarGrams = cookie.sugarGrams;
            this.proteinGrams = cookie.proteinGrams;
            this.carbsGrams = cookie.carbsGrams;
            this.fatGrams = cookie.fatGrams;
            this.createdAt = cookie.createdAt;
            return this;
        }

        public Cookie build() {
            return new Cookie(this);
        }
    }
}