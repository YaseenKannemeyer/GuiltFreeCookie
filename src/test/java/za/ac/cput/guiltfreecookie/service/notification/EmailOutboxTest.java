package za.ac.cput.guiltfreecookie.service.notification;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionTemplate;
import za.ac.cput.guiltfreecookie.domain.EmailDelivery;
import za.ac.cput.guiltfreecookie.repository.EmailDeliveryRepository;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailOutboxTest {
    private final EmailDeliveryRepository repository = mock(EmailDeliveryRepository.class);
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final EmailOutbox outbox = new EmailOutbox(repository, sender, mock(TransactionTemplate.class), true);

    private EmailDelivery pending(LocalDateTime expiry) {
        var delivery = EmailDelivery.queued("sample@example.test", "Confirmation", new byte[]{1, 2, 3}, expiry);
        when(repository.lockById("delivery")).thenReturn(Optional.of(delivery));
        return delivery;
    }

    @Test
    void successfulDeliveryClearsTheStoredMessage() {
        var delivery = pending(LocalDateTime.now().plusHours(24));
        var message = new MimeMessage((Session) null);
        when(sender.createMimeMessage(any(InputStream.class))).thenReturn(message);
        outbox.deliver("delivery");
        verify(sender).send(message);
        assertEquals(EmailDelivery.Status.SENT, delivery.getStatus());
        assertNull(delivery.getPayload());
        assertNotNull(delivery.getSentAt());
    }

    @Test
    void smtpOutageKeepsTheMessageForALaterAttempt() {
        var delivery = pending(LocalDateTime.now().plusHours(24));
        var message = new MimeMessage((Session) null);
        when(sender.createMimeMessage(any(InputStream.class))).thenReturn(message);
        doThrow(new MailSendException("Unavailable")).when(sender).send(message);
        outbox.deliver("delivery");
        assertEquals(EmailDelivery.Status.PENDING, delivery.getStatus());
        assertEquals(1, delivery.getAttempts());
        assertNotNull(delivery.getPayload());
        assertTrue(delivery.getNextAttemptAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void expiredResetLinksAreNeverDelivered() {
        var delivery = pending(LocalDateTime.now().minusMinutes(1));
        outbox.deliver("delivery");
        verifyNoInteractions(sender);
        assertEquals(EmailDelivery.Status.EXPIRED, delivery.getStatus());
        assertNull(delivery.getPayload());
    }

    @Test
    void retriesAreBoundedAndCanBeExplicitlyRestarted() {
        var delivery = EmailDelivery.queued("sample@example.test", "Order", new byte[]{1}, LocalDateTime.now().plusHours(24));
        for (int i = 0; i < 5; i++) delivery.failed(5);
        assertEquals(EmailDelivery.Status.FAILED, delivery.getStatus());
        assertNull(delivery.getNextAttemptAt());
        delivery.retry();
        assertEquals(EmailDelivery.Status.PENDING, delivery.getStatus());
        assertEquals(0, delivery.getAttempts());
    }
}
