package za.ac.cput.guiltfreecookie.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import za.ac.cput.guiltfreecookie.domain.Customer;
import za.ac.cput.guiltfreecookie.domain.Name;
import za.ac.cput.guiltfreecookie.factory.CustomerFactory;
import za.ac.cput.guiltfreecookie.service.CustomerService;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomerControllerTest {

    @Mock
    private CustomerService service;

    @InjectMocks
    private CustomerController controller;
    private Customer customer;
    private Name name;

    @BeforeEach
    void setUp() {
        name = new Name.Builder()
                .setFirstName("Mogamat")
                .setMiddleName("Wazeer")
                .setLastName("Gilbert")
                .build();

        customer = CustomerFactory.createCustomer("wazeerg@gmail.com", name, "0823456789");
    }

    @Test
    void createCustomer() {
        when(service.create(any(Customer.class))).thenReturn(customer);
        ResponseEntity<Customer> response = controller.createCustomer(customer);
        assertNotNull(response);
        assert response.getBody() != null;
        assertEquals("wazeerg@gmail.com", response.getBody().getCustomerEmail());
    }

    @Test
    void getAllCustomers() {
        when(service.getAll()).thenReturn(Arrays.asList(customer));
        ResponseEntity<List<Customer>> response = controller.getAll();
        assertNotNull(response);
        verify(service).getAll();
    }

}

