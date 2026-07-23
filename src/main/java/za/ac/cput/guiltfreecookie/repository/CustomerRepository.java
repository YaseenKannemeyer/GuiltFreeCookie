package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.Customer;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer,String> {
    List<Customer> findAll(String id);
}
