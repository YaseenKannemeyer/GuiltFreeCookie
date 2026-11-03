package za.ac.cput.guiltfreecookie.service.notification;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailSendException;
import org.mockito.ArgumentCaptor;
import java.time.LocalDateTime;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest
class EmailTemplatesTest {

    @Autowired
    private EmailTemplates templates;

    // Also writes every template to target/email-previews/ for a visual check.
    @Test
    void everyTemplateRendersEscapesInputAndStaysUnderGmailsClippingSize() throws Exception {
        Path directory = Path.of("target/email-previews");
        Files.createDirectories(directory);
        for (String name : EmailTemplates.NAMES) {
            Map<String, Object> model = templates.sample(name);
            model.put("name", "<script>alert(1)</script>");

            String html = templates.render(name, model);

            assertTrue(html.length() < 100_000);
            assertFalse(html.contains("<script>"));
            assertFalse(html.contains("th:text"));
            assertTrue(html.contains("wordmark@2x.png"));
            Files.writeString(directory.resolve(name + ".html"), html);
            String text = EmailTemplates.plainText(html);
            assertFalse(text.contains("<table"));
            assertFalse(text.contains("fonts.googleapis"));
        }
    }

    @Test
    void sendsHtmlAndPlainTextWithAnUnsubscribeHeaderForMarketing() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage((Session) null);
        when(sender.createMimeMessage()).thenReturn(message);
        EmailService service = new EmailService(sender, true, "bakery@example.com", templates, mock(EmailOutbox.class), false);
        Map<String, Object> model = templates.sample("password-reset");
        model.put("unsubscribeUrl", "https://example.com/unsubscribe/token");

        service.sendTemplate("sam@example.com", "Reset", "password-reset", model, true);

        message.saveChanges();
        assertEquals("<https://example.com/unsubscribe/token>", message.getHeader("List-Unsubscribe", null));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        message.writeTo(bytes);
        String raw = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains("multipart/alternative"));
        assertTrue(raw.contains("text/plain"));
        assertTrue(raw.contains("text/html"));
        verify(sender).send(message);
    }

    @Test
    void marketingNeedsASafeUnsubscribeLinkAndDisabledMailSendsNothing() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService(sender, false, "bakery@example.com", templates, mock(EmailOutbox.class), false);

        assertThrows(IllegalArgumentException.class,
                () -> service.sendTemplate("sam@example.com", "Hi", "password-reset", templates.sample("password-reset"), true));
        service.sendTemplate("sam@example.com", "Hi", "password-reset", templates.sample("password-reset"), false);

        verifyNoInteractions(sender);
    }

    @Test
    void transactionalTemplatesArePersistedWithTheirLinkExpiry() {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage((Session) null);
        when(sender.createMimeMessage()).thenReturn(message);
        EmailOutbox queue = mock(EmailOutbox.class);
        EmailService service = new EmailService(sender, true, "bakery@example.com", templates, queue, false);
        var model = templates.sample("password-reset");
        model.put("resetMinutes", 10L);
        var before = LocalDateTime.now().plusMinutes(10);
        service.sendTemplate("sam@example.com", "Reset", "password-reset", model, false);
        var expiry = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(queue).enqueue(eq("sam@example.com"), eq("Reset"), eq(message), expiry.capture());
        assertTrue(!expiry.getValue().isBefore(before));
        assertTrue(expiry.getValue().isBefore(before.plusSeconds(5)));
        verify(sender, org.mockito.Mockito.never()).send(any(MimeMessage.class));
    }

    @Test
    void failedMarketingTransportPropagatesToTheFlowRetryHandler() {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage((Session) null);
        when(sender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(message);
        EmailService service = new EmailService(sender, true, "bakery@example.com", templates, mock(EmailOutbox.class), false);
        var model = templates.sample("password-reset");
        model.put("unsubscribeUrl", "https://example.com/unsubscribe");
        assertThrows(MailSendException.class,
                () -> service.sendTemplate("sam@example.com", "Offer", "password-reset", model, true));
    }
}
