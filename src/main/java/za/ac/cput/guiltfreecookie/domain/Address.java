package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class Address {

    private String streetNumber;
    private String streetName;
    private String city;

    // A String, not a number: SA postal codes like 0002 (Pretoria) start with zero.
    @Column(length = 10)
    private String postalCode;

    // Unit, complex or building, e.g. "Unit 4, Oak Court".
    private String complex;
    private String suburb;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Province province;

    protected Address() {
    }

    private Address(Builder builder) {
        this.streetNumber = builder.streetNumber;
        this.streetName = builder.streetName;
        this.city = builder.city;
        this.postalCode = builder.postalCode;
        this.complex = builder.complex;
        this.suburb = builder.suburb;
        this.province = builder.province;
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

    public String getPostalCode() {
        return postalCode;
    }

    public String getComplex() {
        return complex;
    }

    public String getSuburb() {
        return suburb;
    }

    public Province getProvince() {
        return province;
    }

    @Override
    public String toString() {
        return "Address{" +
                "streetNumber='" + streetNumber + '\'' +
                ", streetName='" + streetName + '\'' +
                ", complex='" + complex + '\'' +
                ", suburb='" + suburb + '\'' +
                ", city='" + city + '\'' +
                ", province=" + province +
                ", postalCode='" + postalCode + '\'' +
                '}';
    }

    public static class Builder {
        private String streetNumber;
        private String streetName;
        private String city;
        private String postalCode;
        private String complex;
        private String suburb;
        private Province province;

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

        public Builder setPostalCode(String postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        public Builder setComplex(String complex) {
            this.complex = complex;
            return this;
        }

        public Builder setSuburb(String suburb) {
            this.suburb = suburb;
            return this;
        }

        public Builder setProvince(Province province) {
            this.province = province;
            return this;
        }

        public Builder copy(Address address) {
            this.streetNumber = address.streetNumber;
            this.streetName = address.streetName;
            this.city = address.city;
            this.postalCode = address.postalCode;
            this.complex = address.complex;
            this.suburb = address.suburb;
            this.province = address.province;
            return this;
        }

        public Address build() {
            return new Address(this);
        }
    }
}
