package za.ac.cput.guiltfreecookie.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.ac.cput.guiltfreecookie.domain.Customer;
import za.ac.cput.guiltfreecookie.domain.Name;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CustomerFactoryTest {

    private Name name;

    @BeforeEach
    void setUp(){
        name = new Name.Builder()
                .setFirstName("Mogamat")
                .setMiddleName("Wazeer")
                .setLastName("Gilbert")
                .build();
    }

    @Test
    void testCreateCustomer(){
        Customer customer = new Customer.Builder()
                .setCustomerEmail("wazeerg@gmail.com")
                .setName(name)
                .setMobileNumber("+27869503400")
                .build();

        assertNotNull(customer, "Customer should not be null");
        assertEquals("wazeerg@gmail.com", customer.getCustomerEmail());
        assertEquals("Mogamat", customer.getName().getFirstName());
        assertEquals("+27869503400", customer.getMobileNumber());
        System.out.println(customer);
    }
}
