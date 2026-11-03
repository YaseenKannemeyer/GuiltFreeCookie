package za.ac.cput.guiltfreecookie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.guiltfreecookie.domain.Customer;
import za.ac.cput.guiltfreecookie.domain.Name;
import za.ac.cput.guiltfreecookie.repository.CustomerRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock
    private CustomerRepository repository;

    @InjectMocks
    private CustomerService service;

    private Name name;
    private String testEmail = "wazeerg@gmail.com";

    @BeforeEach
    void setUp() {
        name = new Name.Builder()
                .setFirstName("Mogamat")
                .setMiddleName("Wazeer")
                .setLastName("Gilbert")
                .build();
    }

    @Test
    void testRead(){
        Customer customer = new Customer.Builder()
                .setCustomerEmail(testEmail)
                .setName(name)
                .setMobileNumber("+27869503400")
                .build();

        when(repository.findById(testEmail)).thenReturn(Optional.of(customer));

        Customer result = service.read(testEmail);

        assertNotNull(result, "Customer should not be null");
        assertEquals(testEmail, result.getCustomerEmail());
    }

    @Test
    void updateChangesTheProfileButKeepsTheAccount() {
        Customer existing = new Customer.Builder()
                .setCustomerEmail(testEmail)
                .setName(name)
                .setMobileNumber("0821234567")
                .setPasswordHash("$2a$10$hash")
                .setTokenVersion(2)
                .build();
        Customer changes = new Customer.Builder()
                .setCustomerEmail(testEmail)
                .setName(new Name.Builder().setFirstName("Wazeer").setLastName("Gilbert").build())
                .setMobileNumber("0831234567")
                .build();
        when(repository.findById(testEmail)).thenReturn(Optional.of(existing));
        when(repository.save(org.mockito.ArgumentMatchers.any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        Customer result = service.update(changes);

        assertEquals("Wazeer", result.getName().getFirstName());
        assertEquals("0831234567", result.getMobileNumber());
        assertEquals("$2a$10$hash", result.getPasswordHash());
        assertEquals(2, result.getTokenVersion());
    }

}
