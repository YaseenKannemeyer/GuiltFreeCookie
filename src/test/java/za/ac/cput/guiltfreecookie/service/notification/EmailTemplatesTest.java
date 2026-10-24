package za.ac.cput.guiltfreecookie.service.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
class EmailTemplatesTest {

    @Autowired
    EmailTemplates templates;

    @Test
    void everyPreviewRendersEscapesInputAndStaysBelowClippingSize()
        throws Exception {
        Path directory = Path.of("target/email-previews");
        Files.createDirectories(directory);
        for (String name : EmailTemplates.NAMES) {
            var model = templates.sample(name);
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
    void htmlAndPlainAlternativesAndUnsubscribeHeaderAreSent()
        throws Exception {
        var sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage((Session) null);
        when(sender.createMimeMessage()).thenReturn(message);
        var service = new EmailService(
            sender,
            true,
            "bakery@example.com",
            templates
        );
        var model = templates.sample("password-reset");
        model.put("unsubscribeUrl", "https://example.com/unsubscribe/token");
        service.sendTemplate(
            "sam@example.com",
            "Reset",
            "password-reset",
            model,
            true
        );
        message.saveChanges();
        assertEquals(
            "<https://example.com/unsubscribe/token>",
            message.getHeader("List-Unsubscribe", null)
        );
        var bytes = new java.io.ByteArrayOutputStream();
        message.writeTo(bytes);
        String raw = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(raw.contains("multipart/alternative"));
        assertTrue(raw.contains("text/plain"));
        assertTrue(raw.contains("text/html"));
        verify(sender).send(message);
    }

    @Test
    void marketingRequiresSafeUnsubscribeAndDisabledMailDoesNotSend() {
        var sender = mock(JavaMailSender.class);
        var service = new EmailService(
            sender,
            false,
            "bakery@example.com",
            templates
        );
        assertThrows(IllegalArgumentException.class, () ->
            service.sendTemplate(
                "sam@example.com",
                "Hi",
                "password-reset",
                templates.sample("password-reset"),
                true
            )
        );
        service.sendTemplate(
            "sam@example.com",
            "Hi",
            "password-reset",
            templates.sample("password-reset"),
            false
        );
        verifyNoInteractions(sender);
    }
}
