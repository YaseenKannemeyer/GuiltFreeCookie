package za.ac.cput.guiltfreecookie.controller;

import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.domain.Customer;
import za.ac.cput.guiltfreecookie.service.CustomerService;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    public Customer createCustomer(@RequestBody Customer customer){
        return service.create(customer);
    }

    @GetMapping("/{customerId}")
    public Customer read(@PathVariable String customerId){
        return service.read(customerId);
    }

    @GetMapping
    public List<Customer> getAll(){
        return service.getAll();
    }

    @PutMapping
    public Customer update(@RequestBody Customer customer){
        return service.update(customer);
    }

    @DeleteMapping("/{customerId}")
    public void delete(@PathVariable String customerId){
        service.delete(customerId);
    }
}