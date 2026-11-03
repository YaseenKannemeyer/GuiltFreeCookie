package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// One pack size in a cart. Quantity is in packs; stock is checked in cookies (quantity x unitsPerPack).
@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "variant_id"}),
        indexes = @Index(columnList = "cart_id"))
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cart_item_id")
    private String cartItemId;

    @Column(name = "cart_id", nullable = false)
    private String cartId;

    @Column(name = "variant_id", nullable = false)
    private String variantId;

    @Column(name = "cookie_id", nullable = false)
    private String cookieId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    protected CartItem() {
    }

    private CartItem(Builder builder) {
        this.cartItemId = builder.cartItemId;
        this.cartId = builder.cartId;
        this.variantId = builder.variantId;
        this.cookieId = builder.cookieId;
        this.quantity = builder.quantity;
        this.addedAt = builder.addedAt;
    }

    public String getCartItemId() {
        return cartItemId;
    }

    public String getCartId() {
        return cartId;
    }

    public String getVariantId() {
        return variantId;
    }

    public String getCookieId() {
        return cookieId;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public static class Builder {
        private String cartItemId;
        private String cartId;
        private String variantId;
        private String cookieId;
        private int quantity;
        private LocalDateTime addedAt;

        public Builder setCartItemId(String cartItemId) {
            this.cartItemId = cartItemId;
            return this;
        }

        public Builder setCartId(String cartId) {
            this.cartId = cartId;
            return this;
        }

        public Builder setVariantId(String variantId) {
            this.variantId = variantId;
            return this;
        }

        public Builder setCookieId(String cookieId) {
            this.cookieId = cookieId;
            return this;
        }

        public Builder setQuantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder setAddedAt(LocalDateTime addedAt) {
            this.addedAt = addedAt;
            return this;
        }

        public Builder copy(CartItem item) {
            this.cartItemId = item.cartItemId;
            this.cartId = item.cartId;
            this.variantId = item.variantId;
            this.cookieId = item.cookieId;
            this.quantity = item.quantity;
            this.addedAt = item.addedAt;
            return this;
        }

        public CartItem build() {
            return new CartItem(this);
        }
    }
}
