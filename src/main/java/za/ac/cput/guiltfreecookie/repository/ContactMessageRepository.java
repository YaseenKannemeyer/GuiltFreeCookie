package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.ContactMessage;
import za.ac.cput.guiltfreecookie.domain.ContactStatus;

import java.util.List;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, String> {

    List<ContactMessage> findAllByOrderByCreatedAtDesc();

    List<ContactMessage> findByStatusOrderByCreatedAtDesc(ContactStatus status);
}
