package za.ac.cput.guiltfreecookie.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.domain.Customer;
import za.ac.cput.guiltfreecookie.repository.CustomerRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerService implements IService<Customer, String> {

    private CustomerRepository repository;

    @Autowired
    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public Customer create(Customer customer) {
        if (customer.getCreatedAt() == null) {
            customer = new Customer.Builder().copy(customer).setCreatedAt(LocalDateTime.now()).build();
        }
        return repository.save(customer);
    }

    public Customer read(String id) {
        return repository.findById(id).orElse(null);
    }

    // Updates only: saving an unknown email would quietly create a new customer.
    // Only the profile changes here; account fields (password, token version) are kept.
    public Customer update(Customer customer) {
        Customer existing = customer.getCustomerEmail() == null
                ? null
                : repository.findById(customer.getCustomerEmail()).orElse(null);
        if (existing == null) {
            return null;
        }
        return repository.save(new Customer.Builder()
                .copy(existing)
                .setName(customer.getName())
                .setMobileNumber(customer.getMobileNumber())
                .build());
    }

    public Customer save(Customer customer) {
        return repository.save(customer);
    }

    public boolean delete(String id) {
        this.repository.deleteById(id);
        return true;
    }

    public List<Customer> getAll() {
        return repository.findAll();
    }

}