package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// One review per customer per cookie; only APPROVED reviews are public.
@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cookie_id", "customer_email"}))
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

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    // Never exposed: responses use ReviewView.
    @Column(name = "customer_email", nullable = false, length = 254)
    private String customerEmail;

    @Column(name = "verified_buyer")
    private boolean verifiedBuyer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    protected Review() {
    }

    private Review(Builder builder) {
        this.id = builder.id;
        this.cookieId = builder.cookieId;
        this.rating = builder.rating;
        this.title = builder.title;
        this.body = builder.body;
        this.authorName = builder.authorName;
        this.customerEmail = builder.customerEmail;
        this.verifiedBuyer = builder.verifiedBuyer;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.moderatedAt = builder.moderatedAt;
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

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setCookieId(String cookieId) {
            this.cookieId = cookieId;
            return this;
        }

        public Builder setRating(Integer rating) {
            this.rating = rating;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setBody(String body) {
            this.body = body;
            return this;
        }

        public Builder setAuthorName(String authorName) {
            this.authorName = authorName;
            return this;
        }

        public Builder setCustomerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public Builder setVerifiedBuyer(boolean verifiedBuyer) {
            this.verifiedBuyer = verifiedBuyer;
            return this;
        }

        public Builder setStatus(ReviewStatus status) {
            this.status = status;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder setModeratedAt(LocalDateTime moderatedAt) {
            this.moderatedAt = moderatedAt;
            return this;
        }

        public Builder copy(Review review) {
            this.id = review.id;
            this.cookieId = review.cookieId;
            this.rating = review.rating;
            this.title = review.title;
            this.body = review.body;
            this.authorName = review.authorName;
            this.customerEmail = review.customerEmail;
            this.verifiedBuyer = review.verifiedBuyer;
            this.status = review.status;
            this.createdAt = review.createdAt;
            this.moderatedAt = review.moderatedAt;
            return this;
        }

        public Review build() {
            return new Review(this);
        }
    }
}
