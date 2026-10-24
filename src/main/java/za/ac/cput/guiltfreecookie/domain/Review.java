package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(
        columnNames = { "cookie_id", "customer_email" }
    )
)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "cookie_id", nullable = false)
    private String cookieId;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false, length = 100)
    private String authorName;

    @Column(name = "customer_email", nullable = false, length = 254)
    private String customerEmail;

    private boolean verifiedBuyer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime moderatedAt;

    protected Review() {}

    private Review(Builder b) {
        this.id = b.id;
        this.cookieId = b.cookieId;
        this.rating = b.rating;
        this.title = b.title;
        this.body = b.body;
        this.authorName = b.authorName;
        this.customerEmail = b.customerEmail;
        this.verifiedBuyer = b.verifiedBuyer;
        this.status = b.status;
        this.createdAt = b.createdAt;
        this.moderatedAt = b.moderatedAt;
    }

    public String getId() {
        return id;
    }

    public String getCookieId() {
        return cookieId;
    }

    public Integer getRating() {
        return rating;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public boolean isVerifiedBuyer() {
        return verifiedBuyer;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getModeratedAt() {
        return moderatedAt;
    }

    public static class Builder {

        private String id;
        private String cookieId;
        private Integer rating;
        private String title;
        private String body;
        private String authorName;
        private String customerEmail;
        private boolean verifiedBuyer;
        private ReviewStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime moderatedAt;

        public Builder setId(String value) {
            this.id = value;
            return this;
        }

        public Builder setCookieId(String value) {
            this.cookieId = value;
            return this;
        }

        public Builder setRating(Integer value) {
            this.rating = value;
            return this;
        }

        public Builder setTitle(String value) {
            this.title = value;
            return this;
        }

        public Builder setBody(String value) {
            this.body = value;
            return this;
        }

        public Builder setAuthorName(String value) {
            this.authorName = value;
            return this;
        }

        public Builder setCustomerEmail(String value) {
            this.customerEmail = value;
            return this;
        }

        public Builder setVerifiedBuyer(boolean value) {
            this.verifiedBuyer = value;
            return this;
        }

        public Builder setStatus(ReviewStatus value) {
            this.status = value;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime value) {
            this.createdAt = value;
            return this;
        }

        public Builder setModeratedAt(LocalDateTime value) {
            this.moderatedAt = value;
            return this;
        }

        public Builder copy(Review source) {
            this.id = source.id;
            this.cookieId = source.cookieId;
            this.rating = source.rating;
            this.title = source.title;
            this.body = source.body;
            this.authorName = source.authorName;
            this.customerEmail = source.customerEmail;
            this.verifiedBuyer = source.verifiedBuyer;
            this.status = source.status;
            this.createdAt = source.createdAt;
            this.moderatedAt = source.moderatedAt;
            return this;
        }

        public Review build() {
            return new Review(this);
        }
    }
}
