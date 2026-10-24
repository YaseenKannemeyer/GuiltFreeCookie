package za.ac.cput.guiltfreecookie.service.engagement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.ContactMessage;
import za.ac.cput.guiltfreecookie.domain.ContactStatus;
import za.ac.cput.guiltfreecookie.dto.engagement.ContactRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.ContactMessageRepository;
import za.ac.cput.guiltfreecookie.service.notification.EmailService;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ContactService {

    static final int MAX_NAME_LENGTH = 100;
    static final int MAX_PHONE_LENGTH = 30;
    static final int MAX_SUBJECT_LENGTH = 150;
    static final int MAX_MESSAGE_LENGTH = 5000;

    private final ContactMessageRepository messageRepository;
    private final EmailService emailService;
    private final SiteInfoService siteInfoService;

    public ContactService(ContactMessageRepository messageRepository, EmailService emailService,
                          SiteInfoService siteInfoService) {
        this.messageRepository = messageRepository;
        this.emailService = emailService;
        this.siteInfoService = siteInfoService;
    }

    // Saves the message, emails the shop and sends the customer an auto-reply.
    // A filled-in honeypot is silently dropped so bots can't tell they were caught.
    public void submit(ContactRequest request) {
        if (request.website() != null && !request.website().isBlank()) {
            return;
        }
        String name = Helper.clean(request.name());
        String email = Helper.cleanEmail(request.email());
        String phone = Helper.clean(request.phone());
        String subject = Helper.clean(request.subject());
        String message = Helper.clean(request.message());
        if (!Helper.isPresent(name, MAX_NAME_LENGTH)
                || !Helper.isValidEmail(email)
                || !Helper.isPresent(subject, MAX_SUBJECT_LENGTH)
                || !Helper.isPresent(message, MAX_MESSAGE_LENGTH)) {
            throw new BadRequestException("Enter your name, valid email, subject (" + MAX_SUBJECT_LENGTH
                    + " characters) and message (" + MAX_MESSAGE_LENGTH + " or fewer)");
        }
        if (!Helper.isWithin(phone, MAX_PHONE_LENGTH)
                || (phone != null && !phone.isBlank() && !Helper.isValidMobileNumber(phone))) {
            throw new BadRequestException("Enter a valid South African mobile number");
        }

        ContactMessage saved = messageRepository.save(new ContactMessage.Builder()
                .setName(name)
                .setEmail(email)
                .setPhone(phone)
                .setSubject(subject)
                .setMessage(message)
                .setStatus(ContactStatus.NEW)
                .setCreatedAt(LocalDateTime.now())
                .build());

        Map<String, Object> model = Map.of(
                "name", name,
                "email", email,
                "phone", phone == null ? "" : phone,
                "subject", subject,
                "message", message,
                "messageId", saved.getId());
        emailService.sendTemplate(siteInfoService.setting("support-email"), "New contact message: " + subject,
                "contact-to-shop", model, false);
        emailService.sendTemplate(email, "We received your message", "contact-auto-reply", model, false);
    }

    public List<ContactMessage> list(ContactStatus status) {
        return status == null
                ? messageRepository.findAllByOrderByCreatedAtDesc()
                : messageRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    // Resolving twice keeps the first resolver and time.
    @Transactional
    public ContactMessage resolve(String messageId, String adminId) {
        ContactMessage existing = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));
        if (existing.getStatus() == ContactStatus.RESOLVED) {
            return existing;
        }
        return messageRepository.save(new ContactMessage.Builder()
                .copy(existing)
                .setStatus(ContactStatus.RESOLVED)
                .setResolvedAt(LocalDateTime.now())
                .setResolvedBy(adminId)
                .build());
    }
}
