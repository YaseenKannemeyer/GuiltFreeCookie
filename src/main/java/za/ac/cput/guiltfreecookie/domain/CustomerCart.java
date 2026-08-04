package za.ac.cput.guiltfreecookie.domain;



import java.util.Objects;

public class CustomerCart {

    private String customerEmail;
    private String cartId;

    // Private constructor
    private CustomerCart(Builder builder) {
        this.customerEmail = builder.customerEmail;
        this.cartId = builder.cartId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCartId() {
        return cartId;
    }

    @Override
    public String toString() {
        return "CustomerCart{" +
                "customerEmail='" + customerEmail + '\'' +
                ", cartId='" + cartId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CustomerCart)) return false;
        CustomerCart that = (CustomerCart) obj;
        return Objects.equals(customerEmail, that.customerEmail)
                && Objects.equals(cartId, that.cartId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerEmail, cartId);
    }

    public static class Builder {

        private String customerEmail;
        private String cartId;

        public Builder setCustomerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public Builder setCartId(String cartId) {
            this.cartId = cartId;
            return this;
        }

        public Builder copy(CustomerCart customerCart) {
            this.customerEmail = customerCart.customerEmail;
            this.cartId = customerCart.cartId;
            return this;
        }

        public CustomerCart build() {
            return new CustomerCart(this);
        }
    }
}
/*
 * CustomerCart.java
 * CustomerCart Entity using Builder Pattern
 * Vumane
 */