package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Addresses that asked for no marketing email. Checked before every flow email, subscriber or not.
@Entity
@Table(name = "email_suppressions")
public class EmailSuppression {

    @Id
    @Column(length = 254)
    private String email;

    @Column(length = 50)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected EmailSuppression() {
    }

    private EmailSuppression(Builder builder) {
        this.email = builder.email;
        this.reason = builder.reason;
        this.createdAt = builder.createdAt;
    }

    public String getEmail() {
        return email;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public static class Builder {
        private String email;
        private String reason;
        private LocalDateTime createdAt;

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setReason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder copy(EmailSuppression emailSuppression) {
            this.email = emailSuppression.email;
            this.reason = emailSuppression.reason;
            this.createdAt = emailSuppression.createdAt;
            return this;
        }

        public EmailSuppression build() {
            return new EmailSuppression(this);
        }
    }
}
