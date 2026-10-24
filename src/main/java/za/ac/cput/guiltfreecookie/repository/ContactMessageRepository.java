package za.ac.cput.guiltfreecookie.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.cput.guiltfreecookie.domain.*;

public interface ContactMessageRepository
    extends JpaRepository<ContactMessage, String>
{
    List<ContactMessage> findAllByOrderByCreatedAtDesc();
    List<ContactMessage> findByStatusOrderByCreatedAtDesc(ContactStatus status);
}
