package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_details")
public class DeliveryDetails {

    @Id
    private String deliveryID;

    @Embedded
    private Address address;

    @Column(name = "customer_email", length = 254)
    private String customerEmail;

    @Column(length = 30)
    private String label;

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Column(length = 30)
    private String phone;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected DeliveryDetails() {
    }

    private DeliveryDetails(Builder builder) {
        this.deliveryID = builder.deliveryID;
        this.address = builder.address;
        this.customerEmail = builder.customerEmail;
        this.label = builder.label;
        this.recipientName = builder.recipientName;
        this.phone = builder.phone;
        this.isDefault = builder.isDefault;
        this.createdAt = builder.createdAt;
    }

    public String getDeliveryID() {
        return deliveryID;
    }

    public Address getAddress() {
        return address;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getLabel() {
        return label;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isDefault() {
        return Boolean.TRUE.equals(isDefault);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "DeliveryDetails{" + "deliveryID='" + deliveryID + '\'' + ", address=" + address + '}';
    }

    public static class Builder {

        private String deliveryID;
        private Address address;
        private String customerEmail;
        private String label;
        private String recipientName;
        private String phone;
        private Boolean isDefault;
        private LocalDateTime createdAt;

        public Builder setDeliveryID(String deliveryID) {
            this.deliveryID = deliveryID;
            return this;
        }

        public Builder setAddress(Address address) {
            this.address = address;
            return this;
        }

        public Builder setCustomerEmail(String value) {
            this.customerEmail = value;
            return this;
        }

        public Builder setLabel(String value) {
            this.label = value;
            return this;
        }

        public Builder setRecipientName(String value) {
            this.recipientName = value;
            return this;
        }

        public Builder setPhone(String value) {
            this.phone = value;
            return this;
        }

        public Builder setIsDefault(boolean value) {
            this.isDefault = value;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime value) {
            this.createdAt = value;
            return this;
        }

        public Builder copy(DeliveryDetails deliveryDetails) {
            this.deliveryID = deliveryDetails.deliveryID;
            this.address = deliveryDetails.address;
            this.customerEmail = deliveryDetails.customerEmail;
            this.label = deliveryDetails.label;
            this.recipientName = deliveryDetails.recipientName;
            this.phone = deliveryDetails.phone;
            this.isDefault = deliveryDetails.isDefault;
            this.createdAt = deliveryDetails.createdAt;

            return this;
        }

        public DeliveryDetails build() {
            return new DeliveryDetails(this);
        }
    }
}
