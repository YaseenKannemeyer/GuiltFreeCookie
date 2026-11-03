package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Someone who asked for marketing email. Keeping when they opted in is part
 * of the POPIA record of consent.
 */
@Entity
@Table(name = "newsletter_subscribers")
public class NewsletterSubscriber {

    @Id
    @Column(length = 254)
    private String email;

    @Column(nullable = false)
    private Instant subscribedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SubscriberStatus status;

    // Where they opted in: footer, register or checkout.
    @Column(length = 30)
    private String source;

    // Last open or click on any of our emails; the sunset flow uses it.
    private Instant lastEngagedAt;

    private Instant statusChangedAt;

    protected NewsletterSubscriber() {
    }

    public NewsletterSubscriber(String email, Instant subscribedAt) {
        this.email = email;
        this.subscribedAt = subscribedAt;
        this.status = SubscriberStatus.ACTIVE;
    }

    private NewsletterSubscriber(Builder builder) {
        this.email = builder.email;
        this.subscribedAt = builder.subscribedAt;
        this.status = builder.status;
        this.source = builder.source;
        this.lastEngagedAt = builder.lastEngagedAt;
        this.statusChangedAt = builder.statusChangedAt;
    }

    public String getEmail() {
        return email;
    }

    public Instant getSubscribedAt() {
        return subscribedAt;
    }

    public SubscriberStatus getStatus() {
        return status == null ? SubscriberStatus.ACTIVE : status;
    }

    public String getSource() {
        return source;
    }

    public Instant getLastEngagedAt() {
        return lastEngagedAt;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    public static class Builder {
        private String email;
        private Instant subscribedAt;
        private SubscriberStatus status = SubscriberStatus.ACTIVE;
        private String source;
        private Instant lastEngagedAt;
        private Instant statusChangedAt;

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setSubscribedAt(Instant subscribedAt) {
            this.subscribedAt = subscribedAt;
            return this;
        }

        public Builder setStatus(SubscriberStatus status) {
            this.status = status;
            return this;
        }

        public Builder setSource(String source) {
            this.source = source;
            return this;
        }

        public Builder setLastEngagedAt(Instant lastEngagedAt) {
            this.lastEngagedAt = lastEngagedAt;
            return this;
        }

        public Builder setStatusChangedAt(Instant statusChangedAt) {
            this.statusChangedAt = statusChangedAt;
            return this;
        }

        public Builder copy(NewsletterSubscriber subscriber) {
            this.email = subscriber.email;
            this.subscribedAt = subscriber.subscribedAt;
            this.status = subscriber.getStatus();
            this.source = subscriber.source;
            this.lastEngagedAt = subscriber.lastEngagedAt;
            this.statusChangedAt = subscriber.statusChangedAt;
            return this;
        }

        public NewsletterSubscriber build() {
            return new NewsletterSubscriber(this);
        }
    }
}
