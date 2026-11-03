package za.ac.cput.guiltfreecookie.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @Column(name = "cart_id")
    private String cartId;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "total_price")
    private double totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CartStatus status;

    // True when a signed-in customer owns the cart; a guest who only typed an email doesn't.
    @Column(name = "account_owned")
    private boolean accountOwned;

    @Column(name = "discount_code", length = 30)
    private String discountCode;

    // For the abandoned-cart email link (Phase 7).
    @JsonIgnore
    @Column(name = "recovery_token", length = 64, unique = true)
    private String recoveryToken;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Cart() {
    }

    private Cart(Builder builder) {
        this.cartId = builder.cartId;
        this.customerEmail = builder.customerEmail;
        this.totalPrice = builder.totalPrice;
        this.status = builder.status;
        this.accountOwned = builder.accountOwned;
        this.discountCode = builder.discountCode;
        this.recoveryToken = builder.recoveryToken;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public String getCartId() {
        return cartId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public CartStatus getStatus() {
        return status;
    }

    public boolean isAccountOwned() {
        return accountOwned;
    }

    public String getDiscountCode() {
        return discountCode;
    }

    public String getRecoveryToken() {
        return recoveryToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cart cart)) return false;
        return Double.compare(cart.totalPrice, totalPrice) == 0 &&
                Objects.equals(cartId, cart.cartId) &&
                Objects.equals(customerEmail, cart.customerEmail) &&
                status == cart.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cartId, customerEmail, totalPrice, status);
    }

    @Override
    public String toString() {
        return "Cart{" +
                "cartId='" + cartId + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", totalPrice=" + totalPrice +
                ", status=" + status +
                '}';
    }

    public static class Builder {
        private String cartId;
        private String customerEmail;
        private double totalPrice;
        private CartStatus status;
        private boolean accountOwned;
        private String discountCode;
        private String recoveryToken;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder setCartId(String cartId) {
            this.cartId = cartId;
            return this;
        }

        public Builder setCustomerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public Builder setTotalPrice(double totalPrice) {
            this.totalPrice = totalPrice;
            return this;
        }

        public Builder setStatus(CartStatus status) {
            this.status = status;
            return this;
        }

        public Builder setAccountOwned(boolean accountOwned) {
            this.accountOwned = accountOwned;
            return this;
        }

        public Builder setDiscountCode(String discountCode) {
            this.discountCode = discountCode;
            return this;
        }

        public Builder setRecoveryToken(String recoveryToken) {
            this.recoveryToken = recoveryToken;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder copy(Cart cart) {
            this.cartId = cart.cartId;
            this.customerEmail = cart.customerEmail;
            this.totalPrice = cart.totalPrice;
            this.status = cart.status;
            this.accountOwned = cart.accountOwned;
            this.discountCode = cart.discountCode;
            this.recoveryToken = cart.recoveryToken;
            this.createdAt = cart.createdAt;
            this.updatedAt = cart.updatedAt;
            return this;
        }

        public Cart build() {
            return new Cart(this);
        }
    }
}