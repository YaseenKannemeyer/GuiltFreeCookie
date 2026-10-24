package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

// A snapshot at the time of purchase, so later price or name changes never rewrite past orders.
@Entity
@Table(name = "order_items",
        indexes = {@Index(columnList = "order_id"), @Index(columnList = "cookie_id")})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_item_id")
    private String orderItemId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "cookie_id", nullable = false)
    private String cookieId;

    @Column(name = "variant_id", nullable = false)
    private String variantId;

    @Column(name = "cookie_name", nullable = false)
    private String cookieName;

    @Column(name = "variant_label", nullable = false, length = 50)
    private String variantLabel;

    @Column(name = "units_per_pack", nullable = false)
    private int unitsPerPack;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", precision = 10, scale = 2, nullable = false)
    private BigDecimal lineTotal;

    protected OrderItem() {
    }

    private OrderItem(Builder builder) {
        this.orderItemId = builder.orderItemId;
        this.orderId = builder.orderId;
        this.cookieId = builder.cookieId;
        this.variantId = builder.variantId;
        this.cookieName = builder.cookieName;
        this.variantLabel = builder.variantLabel;
        this.unitsPerPack = builder.unitsPerPack;
        this.unitPrice = builder.unitPrice;
        this.quantity = builder.quantity;
        this.lineTotal = builder.lineTotal;
    }

    public String getOrderItemId() {
        return orderItemId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCookieId() {
        return cookieId;
    }

    public String getVariantId() {
        return variantId;
    }

    public String getCookieName() {
        return cookieName;
    }

    public String getVariantLabel() {
        return variantLabel;
    }

    public int getUnitsPerPack() {
        return unitsPerPack;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public static class Builder {
        private String orderItemId;
        private String orderId;
        private String cookieId;
        private String variantId;
        private String cookieName;
        private String variantLabel;
        private int unitsPerPack;
        private BigDecimal unitPrice;
        private int quantity;
        private BigDecimal lineTotal;

        public Builder setOrderItemId(String orderItemId) {
            this.orderItemId = orderItemId;
            return this;
        }

        public Builder setOrderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder setCookieId(String cookieId) {
            this.cookieId = cookieId;
            return this;
        }

        public Builder setVariantId(String variantId) {
            this.variantId = variantId;
            return this;
        }

        public Builder setCookieName(String cookieName) {
            this.cookieName = cookieName;
            return this;
        }

        public Builder setVariantLabel(String variantLabel) {
            this.variantLabel = variantLabel;
            return this;
        }

        public Builder setUnitsPerPack(int unitsPerPack) {
            this.unitsPerPack = unitsPerPack;
            return this;
        }

        public Builder setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public Builder setQuantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder setLineTotal(BigDecimal lineTotal) {
            this.lineTotal = lineTotal;
            return this;
        }

        public Builder copy(OrderItem orderItem) {
            this.orderItemId = orderItem.orderItemId;
            this.orderId = orderItem.orderId;
            this.cookieId = orderItem.cookieId;
            this.variantId = orderItem.variantId;
            this.cookieName = orderItem.cookieName;
            this.variantLabel = orderItem.variantLabel;
            this.unitsPerPack = orderItem.unitsPerPack;
            this.unitPrice = orderItem.unitPrice;
            this.quantity = orderItem.quantity;
            this.lineTotal = orderItem.lineTotal;
            return this;
        }

        public OrderItem build() {
            return new OrderItem(this);
        }
    }
}
