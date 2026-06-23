package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Customer {

    @Id
    private String customerEmail;

    @Embedded
    private Name name;

    private String mobileNumber;

    protected Customer() {}

    private Customer(Builder builder) {
        this.customerEmail = builder.customerEmail;
        this.name = builder.name;
        this.mobileNumber = builder.mobileNumber;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public Name getName() {
        return name;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public static class Builder {
        private String customerEmail;
        private Name name;
        private String mobileNumber;

        public Builder setCustomerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public Builder setName(Name name) {
            this.name = name;
            return this;
        }

        public Builder setMobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
            return this;
        }

        public Builder copy(Customer customer) {
            this.customerEmail = customer.getCustomerEmail();
            this.name = customer.getName();
            this.mobileNumber = customer.getMobileNumber();
            return this;
        }

        public Customer build() {return new Customer(this);}
    }
}