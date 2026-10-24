package za.ac.cput.guiltfreecookie.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @Column(name = "customer_email")
    private String customerEmail;

    @Embedded
    private Name name;

    @Column(name = "mobile_number")
    private String mobileNumber;

    // Null for customers without an online account (created by an admin).
    @JsonIgnore
    @Column(name = "password_hash")
    private String passwordHash;

    // Same scheme as Admin.tokenVersion: bumped on password change or reset.
    @JsonIgnore
    @Column(name = "token_version")
    private Integer tokenVersion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected Customer() {}

    private Customer(Builder builder) {
        this.customerEmail = builder.customerEmail;
        this.name = builder.name;
        this.mobileNumber = builder.mobileNumber;
        this.passwordHash = builder.passwordHash;
        this.tokenVersion = builder.tokenVersion;
        this.createdAt = builder.createdAt;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public Name getName() {
        return name;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getTokenVersion() {
        return tokenVersion == null ? 0 : tokenVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @JsonIgnore
    public boolean hasAccount() {
        return passwordHash != null;
    }

    public static class Builder {
        private String customerEmail;
        private Name name;
        private String mobileNumber;
        private String passwordHash;
        private Integer tokenVersion;
        private LocalDateTime createdAt;

        public Builder setCustomerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public Builder setName(Name name) {
            this.name = name;
            return this;
        }

        public Builder setMobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
            return this;
        }

        public Builder setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder setTokenVersion(int tokenVersion) {
            this.tokenVersion = tokenVersion;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder copy(Customer customer) {
            this.customerEmail = customer.customerEmail;
            this.name = customer.name;
            this.mobileNumber = customer.mobileNumber;
            this.passwordHash = customer.passwordHash;
            this.tokenVersion = customer.tokenVersion;
            this.createdAt = customer.createdAt;
            return this;
        }

        public Customer build() {return new Customer(this);}
    }
}
