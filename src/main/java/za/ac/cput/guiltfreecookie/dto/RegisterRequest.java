package za.ac.cput.guiltfreecookie.dto;

import za.ac.cput.guiltfreecookie.domain.Name;

public class RegisterRequest {
    private String customerEmail;
    private String password;
    private String mobileNumber;
    private Name name;

    protected RegisterRequest(){}

    private RegisterRequest(Builder builder) {
        this.customerEmail = builder.customerEmail;
        this.password = builder.password;
        this.mobileNumber = builder.mobileNumber;
        this.name = builder.name;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getPassword() {
        return password;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public Name getName() {
        return name;
    }

    public static class Builder {
        private String customerEmail;
        private String password;
        private String mobileNumber;
        private Name name;

        public Builder setCustomerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setMobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
            return this;
        }

        public Builder setName(Name name) {
            this.name = name;
            return this;
        }

        public Builder copy(RegisterRequest registerRequest) {
            this.customerEmail = registerRequest.customerEmail;
            this.password = registerRequest.password;
            this.mobileNumber = registerRequest.mobileNumber;
            this.name = registerRequest.name;
            return this;
        }

        public RegisterRequest build() {return new RegisterRequest(this);}
    }
}
