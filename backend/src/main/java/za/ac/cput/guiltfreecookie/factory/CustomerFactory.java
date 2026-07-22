package za.ac.cput.guiltfreecookie.factory;

import za.ac.cput.guiltfreecookie.domain.Customer;
import za.ac.cput.guiltfreecookie.domain.Name;
import za.ac.cput.guiltfreecookie.util.Helper;

public class CustomerFactory {
    public static Customer createCustomer(String customerEmail, Name name, String mobileNumber){
        if(Helper.isNullOrEmpty(customerEmail)){
            throw new NullPointerException("Customer Email cannot be empty");
        }
        if (!Helper.isValidEmail(customerEmail)) {
            throw new IllegalArgumentException("Please Enter valid Email");
        }
        if (name == null || Helper.isNullOrEmpty(name.getFirstName()) || Helper.isNullOrEmpty(name.getLastName())) {
            throw new IllegalArgumentException("Please Enter a Name and Surname");
        }
        if (!Helper.isValidMobileNumber(mobileNumber)) {
            throw new IllegalArgumentException("Please Enter valid Mobile Number");
        }

        String cleanEmail = customerEmail.trim().toLowerCase();

        return new Customer.Builder()
                .setCustomerEmail(cleanEmail)
                .setName(name)
                .setMobileNumber(mobileNumber)
                .build();
    }

}
