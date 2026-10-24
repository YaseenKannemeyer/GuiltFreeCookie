package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

// Extra gallery photos. Cookie.image stays the primary/thumbnail image.
@Entity
@Table(name = "cookie_images")
public class CookieImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "image_id")
    private String imageId;

    @Column(name = "cookie_id", nullable = false)
    private String cookieId;

    @Column(nullable = false)
    private String image;

    @Column(name = "alt_text", length = 150)
    private String altText;

    @Column(name = "sort_order")
    private int sortOrder;

    protected CookieImage() {
    }

    private CookieImage(Builder builder) {
        this.imageId = builder.imageId;
        this.cookieId = builder.cookieId;
        this.image = builder.image;
        this.altText = builder.altText;
        this.sortOrder = builder.sortOrder;
    }

    public String getImageId() {
        return imageId;
    }

    public String getCookieId() {
        return cookieId;
    }

    public String getImage() {
        return image;
    }

    public String getAltText() {
        return altText;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public static class Builder {
        private String imageId;
        private String cookieId;
        private String image;
        private String altText;
        private int sortOrder;

        public Builder setImageId(String imageId) {
            this.imageId = imageId;
            return this;
        }

        public Builder setCookieId(String cookieId) {
            this.cookieId = cookieId;
            return this;
        }

        public Builder setImage(String image) {
            this.image = image;
            return this;
        }

        public Builder setAltText(String altText) {
            this.altText = altText;
            return this;
        }

        public Builder setSortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder copy(CookieImage cookieImage) {
            this.imageId = cookieImage.imageId;
            this.cookieId = cookieImage.cookieId;
            this.image = cookieImage.image;
            this.altText = cookieImage.altText;
            this.sortOrder = cookieImage.sortOrder;
            return this;
        }

        public CookieImage build() {
            return new CookieImage(this);
        }
    }
}
