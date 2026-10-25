package za.ac.cput.guiltfreecookie.repository.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import za.ac.cput.guiltfreecookie.domain.Customer;

import java.util.Optional;

public interface AccountCustomerRepository extends Repository<Customer, String> {

    // Locks the customer even when their address book is empty, so concurrent saves can't pass the cap.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.customerEmail = :email")
    Optional<Customer> lock(@Param("email") String email);
}
