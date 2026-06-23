package za.ac.cput.guiltfreecookie.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.domain.Customer;
import za.ac.cput.guiltfreecookie.repository.CustomerRepository;

import java.util.List;

@Service
public class CustomerService implements IService<Customer, String> {

    private CustomerRepository repository;

    @Autowired
    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public Customer create(Customer customer) {
        return repository.save(customer);
    }

    public Customer read(String id) {
        return repository.findById(id).orElse(null);
    }

    public Customer update(Customer customer) {
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
