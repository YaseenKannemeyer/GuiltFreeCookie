package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Durable transactional email. Payloads are erased after delivery or expiry. */
@Entity
@Table(name = "email_deliveries", indexes = @Index(name = "idx_mail_due", columnList = "status,next_attempt_at"))
public class EmailDelivery {
    public enum Status { PENDING, SENT, FAILED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false, length = 254)
    private String recipient;
    @Column(nullable = false, length = 200)
    private String subject;
    @Lob
    @Column(columnDefinition = "longblob")
    private byte[] payload;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;
    private int attempts;
    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime sentAt;

    protected EmailDelivery() { }

    public static EmailDelivery queued(String recipient, String subject, byte[] payload, LocalDateTime expiresAt) {
        EmailDelivery delivery = new EmailDelivery();
        delivery.recipient = recipient;
        delivery.subject = subject.length() > 200 ? subject.substring(0, 200) : subject;
        delivery.payload = payload;
        delivery.status = Status.PENDING;
        delivery.createdAt = LocalDateTime.now();
        delivery.nextAttemptAt = delivery.createdAt;
        delivery.expiresAt = expiresAt;
        return delivery;
    }

    public void sent() {
        attempts++;
        status = Status.SENT;
        sentAt = LocalDateTime.now();
        payload = null;
        nextAttemptAt = null;
    }

    public void failed(int maxAttempts) {
        attempts++;
        status = attempts >= maxAttempts ? Status.FAILED : Status.PENDING;
        long delayMinutes = switch (attempts) { case 1 -> 1; case 2 -> 5; case 3 -> 15; default -> 60; };
        nextAttemptAt = status == Status.PENDING ? LocalDateTime.now().plusMinutes(delayMinutes) : null;
    }

    public void expire() { status = Status.EXPIRED; payload = null; nextAttemptAt = null; }
    public void retry() { status = Status.PENDING; attempts = 0; nextAttemptAt = LocalDateTime.now(); }
    public String getId() { return id; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public byte[] getPayload() { return payload; }
    public Status getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getSentAt() { return sentAt; }
}
