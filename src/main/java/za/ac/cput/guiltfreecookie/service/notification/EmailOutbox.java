package za.ac.cput.guiltfreecookie.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import za.ac.cput.guiltfreecookie.domain.EmailDelivery;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.EmailDeliveryRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmailOutbox {
    private static final Logger log = LoggerFactory.getLogger(EmailOutbox.class);
    private final EmailDeliveryRepository repository;
    private final JavaMailSender sender;
    private final TransactionTemplate transactions;
    private final boolean enabled;

    public EmailOutbox(EmailDeliveryRepository repository, JavaMailSender sender, TransactionTemplate transactions,
                       @Value("${app.mail.enabled}") boolean enabled) {
        this.repository = repository;
        this.sender = sender;
        this.transactions = transactions;
        this.enabled = enabled;
    }

    // AFTER_COMMIT order listeners need a new transaction for a durable write.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueue(String recipient, String subject, MimeMessage message, LocalDateTime expiresAt) {
        try {
            message.saveChanges();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            message.writeTo(bytes);
            repository.save(EmailDelivery.queued(recipient, subject, bytes.toByteArray(), expiresAt));
        } catch (MessagingException | IOException e) {
            throw new IllegalStateException("Could not queue email", e);
        }
    }

    @Scheduled(fixedDelayString = "${app.mail.delivery-check-ms:5000}", initialDelayString = "${app.mail.delivery-check-ms:5000}")
    public void deliverDue() {
        if (!enabled) return;
        var due = repository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                EmailDelivery.Status.PENDING, LocalDateTime.now(), PageRequest.of(0, 25));
        for (var delivery : due) {
            try {
                transactions.executeWithoutResult(status -> deliver(delivery.getId()));
            } catch (RuntimeException e) {
                log.error("Email queue processing failed for {} ({})", delivery.getId(), e.getClass().getSimpleName());
            }
        }
    }

    void deliver(String id) {
        var delivery = repository.lockById(id).orElseThrow();
        if (delivery.getStatus() != EmailDelivery.Status.PENDING
                || delivery.getNextAttemptAt().isAfter(LocalDateTime.now())) return;
        if (!delivery.getExpiresAt().isAfter(LocalDateTime.now())) {
            delivery.expire();
        } else {
            try {
                sender.send(sender.createMimeMessage(new ByteArrayInputStream(delivery.getPayload())));
                delivery.sent();
            } catch (MailException e) {
                delivery.failed(5);
                log.warn("Email delivery {} failed on attempt {} ({})", id, delivery.getAttempts(), e.getClass().getSimpleName());
            }
        }
        repository.save(delivery);
    }

    @Transactional
    public void retry(String id) {
        var delivery = repository.lockById(id).orElseThrow(() -> new NotFoundException("Email not found"));
        if (!enabled) throw new ConflictException("Email delivery is disabled in this environment");
        if (delivery.getStatus() != EmailDelivery.Status.FAILED || delivery.getPayload() == null
                || !delivery.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ConflictException("Only a failed, unexpired email can be retried. Request a new link if it has expired.");
        }
        delivery.retry();
        repository.save(delivery);
    }

    public record DeliveryView(String id, String recipient, String subject, EmailDelivery.Status status,
                               int attempts, LocalDateTime createdAt, LocalDateTime sentAt, boolean canRetry) { }

    @Transactional(readOnly = true)
    public List<DeliveryView> recent() {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 50)).stream().map(d -> new DeliveryView(
                d.getId(), d.getRecipient(), d.getSubject(), d.getStatus(), d.getAttempts(), d.getCreatedAt(), d.getSentAt(),
                enabled && d.getStatus() == EmailDelivery.Status.FAILED && d.getPayload() != null
                        && d.getExpiresAt().isAfter(LocalDateTime.now()))).toList();
    }

    @Scheduled(fixedDelayString = "${app.mail.cleanup-ms:3600000}")
    @Transactional
    public void cleanUp() {
        for (var d : repository.findByStatusInAndExpiresAtBefore(
                List.of(EmailDelivery.Status.PENDING, EmailDelivery.Status.FAILED), LocalDateTime.now(), PageRequest.of(0, 100))) {
            d.expire();
            repository.save(d);
        }
        repository.deleteByCreatedAtBeforeAndPayloadIsNull(LocalDateTime.now().minusDays(7));
    }
}
