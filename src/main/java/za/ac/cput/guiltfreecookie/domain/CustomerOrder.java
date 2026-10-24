package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Named CustomerOrder because ORDER is a reserved word in JPQL. Money is VAT-inclusive rand.
@Entity
@Table(name = "orders",
        indexes = {@Index(columnList = "customer_email"), @Index(columnList = "status, reserved_until")})
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private String orderId;

    // What customers see, e.g. GFC-261024-7K3Q.
    @Column(name = "order_number", nullable = false, unique = true, length = 20)
    private String orderNumber;


    @Column(name = "customer_email", nullable = false, length = 254)
    private String customerEmail;

    // Placed while signed in, so it shows under the customer's account.
    @Column(name = "account_owned")
    private boolean accountOwned;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_method", nullable = false, length = 20)
    private ShippingMethod shippingMethod;

    // Courier orders only.
    @Embedded
    private Address address;

    // PUDO orders: the locker the customer chose.
    @Column(name = "pudo_locker", length = 150)
    private String pudoLocker;

    @Column(name = "customer_note", length = 500)
    private String customerNote;

    @Column(name = "cart_id")
    private String cartId;

    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "discount_code", length = 30)
    private String discountCode;

    @Column(name = "discount_total", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountTotal;

    @Column(name = "shipping_total", precision = 10, scale = 2, nullable = false)
    private BigDecimal shippingTotal;

    @Column(name = "vat_included", precision = 10, scale = 2)
    private BigDecimal vatIncluded;

    @Column(name = "total", precision = 10, scale = 2, nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    // Stock is held until then; unpaid orders are cancelled after it.
    @Column(name = "reserved_until")
    private LocalDateTime reservedUntil;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "courier_name", length = 100)
    private String courierName;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected CustomerOrder() {
    }

    private CustomerOrder(Builder builder) {
        this.orderId = builder.orderId;
        this.orderNumber = builder.orderNumber;
        this.customerEmail = builder.customerEmail;
        this.accountOwned = builder.accountOwned;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.phone = builder.phone;
        this.shippingMethod = builder.shippingMethod;
        this.address = builder.address;
        this.pudoLocker = builder.pudoLocker;
        this.customerNote = builder.customerNote;
        this.cartId = builder.cartId;
        this.subtotal = builder.subtotal;
        this.discountCode = builder.discountCode;
        this.discountTotal = builder.discountTotal;
        this.shippingTotal = builder.shippingTotal;
        this.vatIncluded = builder.vatIncluded;
        this.total = builder.total;
        this.status = builder.status;
        this.reservedUntil = builder.reservedUntil;
        this.paidAt = builder.paidAt;
        this.courierName = builder.courierName;
        this.trackingNumber = builder.trackingNumber;
        this.trackingUrl = builder.trackingUrl;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }


    public String getCustomerEmail() {
        return customerEmail;
    }

    public boolean isAccountOwned() {
        return accountOwned;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public ShippingMethod getShippingMethod() {
        return shippingMethod;
    }

    public Address getAddress() {
        return address;
    }

    public String getPudoLocker() {
        return pudoLocker;
    }

    public String getCustomerNote() {
        return customerNote;
    }

    public String getCartId() {
        return cartId;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public String getDiscountCode() {
        return discountCode;
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    public BigDecimal getShippingTotal() {
        return shippingTotal;
    }

    public BigDecimal getVatIncluded() {
        return vatIncluded;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getReservedUntil() {
        return reservedUntil;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public String getCourierName() {
        return courierName;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getTrackingUrl() {
        return trackingUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static class Builder {
        private String orderId;
        private String orderNumber;
        private String customerEmail;
        private boolean accountOwned;
        private String firstName;
        private String lastName;
        private String phone;
        private ShippingMethod shippingMethod;
        private Address address;
        private String pudoLocker;
        private String customerNote;
        private String cartId;
        private BigDecimal subtotal;
        private String discountCode;
        private BigDecimal discountTotal;
        private BigDecimal shippingTotal;
        private BigDecimal vatIncluded;
        private BigDecimal total;
        private OrderStatus status;
        private LocalDateTime reservedUntil;
        private LocalDateTime paidAt;
        private String courierName;
        private String trackingNumber;
        private String trackingUrl;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder setOrderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
            return this;
        }


        public Builder setCustomerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public Builder setAccountOwned(boolean accountOwned) {
            this.accountOwned = accountOwned;
            return this;
        }

        public Builder setFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder setLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder setPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder setShippingMethod(ShippingMethod shippingMethod) {
            this.shippingMethod = shippingMethod;
            return this;
        }

        public Builder setAddress(Address address) {
            this.address = address;
            return this;
        }

        public Builder setPudoLocker(String pudoLocker) {
            this.pudoLocker = pudoLocker;
            return this;
        }

        public Builder setCustomerNote(String customerNote) {
            this.customerNote = customerNote;
            return this;
        }

        public Builder setCartId(String cartId) {
            this.cartId = cartId;
            return this;
        }

        public Builder setSubtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
            return this;
        }

        public Builder setDiscountCode(String discountCode) {
            this.discountCode = discountCode;
            return this;
        }

        public Builder setDiscountTotal(BigDecimal discountTotal) {
            this.discountTotal = discountTotal;
            return this;
        }

        public Builder setShippingTotal(BigDecimal shippingTotal) {
            this.shippingTotal = shippingTotal;
            return this;
        }

        public Builder setVatIncluded(BigDecimal vatIncluded) {
            this.vatIncluded = vatIncluded;
            return this;
        }

        public Builder setTotal(BigDecimal total) {
            this.total = total;
            return this;
        }

        public Builder setStatus(OrderStatus status) {
            this.status = status;
            return this;
        }

        public Builder setReservedUntil(LocalDateTime reservedUntil) {
            this.reservedUntil = reservedUntil;
            return this;
        }

        public Builder setPaidAt(LocalDateTime paidAt) {
            this.paidAt = paidAt;
            return this;
        }

        public Builder setCourierName(String courierName) {
            this.courierName = courierName;
            return this;
        }

        public Builder setTrackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
            return this;
        }

        public Builder setTrackingUrl(String trackingUrl) {
            this.trackingUrl = trackingUrl;
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

        public Builder copy(CustomerOrder customerOrder) {
            this.orderId = customerOrder.orderId;
            this.orderNumber = customerOrder.orderNumber;
            this.customerEmail = customerOrder.customerEmail;
            this.accountOwned = customerOrder.accountOwned;
            this.firstName = customerOrder.firstName;
            this.lastName = customerOrder.lastName;
            this.phone = customerOrder.phone;
            this.shippingMethod = customerOrder.shippingMethod;
            this.address = customerOrder.address;
            this.pudoLocker = customerOrder.pudoLocker;
            this.customerNote = customerOrder.customerNote;
            this.cartId = customerOrder.cartId;
            this.subtotal = customerOrder.subtotal;
            this.discountCode = customerOrder.discountCode;
            this.discountTotal = customerOrder.discountTotal;
            this.shippingTotal = customerOrder.shippingTotal;
            this.vatIncluded = customerOrder.vatIncluded;
            this.total = customerOrder.total;
            this.status = customerOrder.status;
            this.reservedUntil = customerOrder.reservedUntil;
            this.paidAt = customerOrder.paidAt;
            this.courierName = customerOrder.courierName;
            this.trackingNumber = customerOrder.trackingNumber;
            this.trackingUrl = customerOrder.trackingUrl;
            this.createdAt = customerOrder.createdAt;
            this.updatedAt = customerOrder.updatedAt;
            return this;
        }

        public CustomerOrder build() {
            return new CustomerOrder(this);
        }
    }
}
