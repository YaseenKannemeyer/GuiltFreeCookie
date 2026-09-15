package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class Address {

    private String streetNumber;
    private String streetName;
    private String city;
    private Long postalCode;

    protected Address() {
    }

    private Address(Builder builder) {
        this.streetNumber = builder.streetNumber;
        this.streetName = builder.streetName;
        this.city = builder.city;
        this.postalCode = builder.postalCode;
    }

    public String getStreetNumber() {
        return streetNumber;
    }

    public String getStreetName() {
        return streetName;
    }

    public String getCity() {
        return city;
    }

    public Long getPostalCode() {
        return postalCode;
    }

    @Override
    public String toString() {
        return "Address{" +
                "streetNumber='" + streetNumber + '\'' +
                ", streetName='" + streetName + '\'' +
                ", city='" + city + '\'' +
                ", postalCode=" + postalCode +
                '}';
    }

    public static class Builder {
        private String streetNumber;
        private String streetName;
        private String city;
        private Long postalCode;

        public Builder setStreetNumber(String streetNumber) {
            this.streetNumber = streetNumber;
            return this;
        }

        public Builder setStreetName(String streetName) {
            this.streetName = streetName;
            return this;
        }

        public Builder setCity(String city) {
            this.city = city;
            return this;
        }

        public Builder setPostalCode(Long postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        public Builder copy(Address address) {
            this.streetNumber = address.streetNumber;
            this.streetName = address.streetName;
            this.city = address.city;
            this.postalCode = address.postalCode;
            return this;
        }

        public Address build() {
            return new Address(this);
        }
    }
}

