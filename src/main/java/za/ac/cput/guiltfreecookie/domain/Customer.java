package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer  {

    @Id
    @Column(name = "customer_email")
    private String customerEmail;

    @Embedded
    private Name name;

    @Column(name = "mobile_number")
    private String mobileNumber;

    private String password;

    protected Customer() {}

    private Customer(Builder builder) {
        this.customerEmail = builder.customerEmail;
        this.name = builder.name;
        this.mobileNumber = builder.mobileNumber;
        this.password = builder.password;
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

    public String getPassword() {
        return password;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    public boolean isAccountNonExpired() {
        return true;
    }

    public boolean isAccountNonLocked() {
        return true;
    }

    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean isEnabled() {
        return true;
    }

    public static class Builder {
        private String customerEmail;
        private Name name;
        private String mobileNumber;
        private String password;

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

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder copy(Customer customer) {
            this.customerEmail = customer.getCustomerEmail();
            this.name = customer.getName();
            this.mobileNumber = customer.getMobileNumber();
            this.password = customer.getPassword();
            return this;
        }

        public Customer build() {return new Customer(this);}
    }
}
