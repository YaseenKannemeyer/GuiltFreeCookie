package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "delivery_details")
public class DeliveryDetails {

    @Id
    private String deliveryID;

    @Embedded
    private Address address;

    protected DeliveryDetails() {
    }

    private DeliveryDetails(Builder builder) {
        this.deliveryID = builder.deliveryID;
        this.address = builder.address;
    }

    public String getDeliveryID() {
        return deliveryID;
    }

    public Address getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "DeliveryDetails{" +
                "deliveryID='" + deliveryID + '\'' +
                ", address=" + address +
                '}';
    }

    public static class Builder {
        private String deliveryID;
        private Address address;

        public Builder setDeliveryID(String deliveryID) {
            this.deliveryID = deliveryID;
            return this;
        }

        public Builder setAddress(Address address) {
            this.address = address;
            return this;
        }

        public Builder copy(DeliveryDetails deliveryDetails) {
            this.deliveryID = deliveryDetails.deliveryID;
            this.address = deliveryDetails.address;
            return this;
        }

        public DeliveryDetails build() {
            return new DeliveryDetails(this);
        }
    }
}