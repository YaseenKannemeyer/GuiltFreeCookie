package za.ac.cput.guiltfreecookie.domain;



import java.util.Objects;

public class CookieCart {

    private String cookieId;
    private String cartId;

    // Private constructor
    private CookieCart(Builder builder) {
        this.cookieId = builder.cookieId;
        this.cartId = builder.cartId;
    }

    public String getCookieId() {
        return cookieId;
    }

    public String getCartId() {
        return cartId;
    }

    @Override
    public String toString() {
        return "CookieCart{" +
                "cookieId='" + cookieId + '\'' +
                ", cartId='" + cartId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CookieCart)) return false;
        CookieCart that = (CookieCart) obj;
        return Objects.equals(cookieId, that.cookieId)
                && Objects.equals(cartId, that.cartId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cookieId, cartId);
    }

    public static class Builder {

        private String cookieId;
        private String cartId;

        public Builder setCookieId(String cookieId) {
            this.cookieId = cookieId;
            return this;
        }

        public Builder setCartId(String cartId) {
            this.cartId = cartId;
            return this;
        }

        public Builder copy(CookieCart cookieCart) {
            this.cookieId = cookieCart.cookieId;
            this.cartId = cookieCart.cartId;
            return this;
        }

        public CookieCart build() {
            return new CookieCart(this);
        }
    }
}
/*
 * CookieCart.java
 * CookieCart Entity using Builder Patter
 * Vumane
 */