package za.ac.cput.guiltfreecookie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.guiltfreecookie.domain.Customer;
import za.ac.cput.guiltfreecookie.domain.Name;
import za.ac.cput.guiltfreecookie.repository.CustomerRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

        Mockito.when(repository.findById(testEmail)).thenReturn(Optional.of(customer));

        Customer result = service.read(testEmail);

        assertNotNull(result, "Customer should not be null");
        assertEquals(testEmail, result.getCustomerEmail());
    }

}
