package za.ac.cput.guiltfreecookie.service.engagement;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.*;
import za.ac.cput.guiltfreecookie.dto.engagement.ContactRequest;
import za.ac.cput.guiltfreecookie.exception.*;
import za.ac.cput.guiltfreecookie.repository.ContactMessageRepository;
import za.ac.cput.guiltfreecookie.service.notification.EmailService;
import za.ac.cput.guiltfreecookie.util.Helper;

@Service
public class ContactService {

    private final ContactMessageRepository messages;
    private final EmailService email;
    private final SiteInfoService site;

    public ContactService(
        ContactMessageRepository messages,
        EmailService email,
        SiteInfoService site
    ) {
        this.messages = messages;
        this.email = email;
        this.site = site;
    }

    public void submit(ContactRequest r) {
        if (r.website() != null && !r.website().isBlank()) return;
        String name = Helper.clean(r.name()),
            address = Helper.cleanEmail(r.email()),
            phone = Helper.clean(r.phone());
        String subject = Helper.clean(r.subject()),
            body = Helper.clean(r.message());
        if (
            !Helper.isPresent(name, 100) ||
            !Helper.isValidEmail(address) ||
            !Helper.isPresent(subject, 150) ||
            !Helper.isPresent(body, 5000)
        ) throw new BadRequestException(
            "Enter your name, valid email, subject (150 characters) and message (5000 or fewer)"
        );
        if (
            !Helper.isWithin(phone, 30) ||
            (phone != null &&
                !phone.isBlank() &&
                !Helper.isValidMobileNumber(phone))
        ) throw new BadRequestException(
            "Enter a valid South African mobile number"
        );
        ContactMessage saved = messages.save(
            new ContactMessage.Builder()
                .setName(name)
                .setEmail(address)
                .setPhone(phone)
                .setSubject(subject)
                .setMessage(body)
                .setStatus(ContactStatus.NEW)
                .setCreatedAt(LocalDateTime.now())
                .build()
        );
        Map<String, Object> model = Map.of(
            "name",
            name,
            "email",
            address,
            "phone",
            phone == null ? "" : phone,
            "subject",
            subject,
            "message",
            body,
            "messageId",
            saved.getId()
        );
        email.sendTemplate(
            site.setting("support-email"),
            "New contact message: " + subject,
            "contact-to-shop",
            model,
            false
        );
        email.sendTemplate(
            address,
            "We received your message",
            "contact-auto-reply",
            model,
            false
        );
    }

    public List<ContactMessage> list(ContactStatus status) {
        return status == null
            ? messages.findAllByOrderByCreatedAtDesc()
            : messages.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public ContactMessage resolve(String id, String adminId) {
        ContactMessage existing = messages
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Message not found"));
        if (existing.getStatus() == ContactStatus.RESOLVED) return existing;
        return messages.save(
            new ContactMessage.Builder()
                .copy(existing)
                .setStatus(ContactStatus.RESOLVED)
                .setResolvedAt(LocalDateTime.now())
                .setResolvedBy(adminId)
                .build()
        );
    }
}
