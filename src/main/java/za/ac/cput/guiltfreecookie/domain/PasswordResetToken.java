package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Only the SHA-256 of the emailed token is stored, so a database leak can't be used to reset passwords.
// Single use: a successful reset deletes every token for that customer.
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "customer_email", nullable = false, length = 254)
    private String customerEmail;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected PasswordResetToken() {
    }

    private PasswordResetToken(Builder builder) {
        this.tokenHash = builder.tokenHash;
        this.customerEmail = builder.customerEmail;
        this.expiresAt = builder.expiresAt;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public static class Builder {
        private String tokenHash;
        private String customerEmail;
        private LocalDateTime expiresAt;

        public Builder setTokenHash(String tokenHash) {
            this.tokenHash = tokenHash;
            return this;
        }

        public Builder setCustomerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public Builder setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder copy(PasswordResetToken token) {
            this.tokenHash = token.tokenHash;
            this.customerEmail = token.customerEmail;
            this.expiresAt = token.expiresAt;
            return this;
        }

        public PasswordResetToken build() {
            return new PasswordResetToken(this);
        }
    }
}
