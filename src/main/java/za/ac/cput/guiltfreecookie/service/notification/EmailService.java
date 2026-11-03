package za.ac.cput.guiltfreecookie.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;
    private final EmailTemplates templates;
    private final EmailOutbox outbox;
    private final boolean capture;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.enabled}") boolean enabled,
            @Value("${app.mail.from}") String from,
            EmailTemplates templates, EmailOutbox outbox,
            @Value("${app.demo.enabled:false}") boolean capture) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.templates = templates;
        this.outbox = outbox;
        this.capture = capture;
    }

    public void send(String to, String subject, String body) {
        if (!enabled && !capture) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            outbox.enqueue(to, subject, message, LocalDateTime.now().plusHours(24));
        } catch (MessagingException e) {
            throw new IllegalStateException("Could not prepare email", e);
        }
    }

    // Branded HTML plus a plain-text alternative. Marketing email must carry an HTTPS unsubscribe link (plain
    // HTTP only on localhost); oneClickUnsubscribeUrl, when given, goes in the List-Unsubscribe header so mail
    // apps can unsubscribe in one tap (RFC 8058). Bodies are never logged (they can hold reset links).
    public void sendTemplate(String to, String subject, String template, Map<String, Object> model, boolean marketing) {
        Map<String, Object> values = new HashMap<>(model);
        values.put("marketing", marketing);
        String unsubscribeUrl = Objects.toString(model.get("unsubscribeUrl"), "");
        String headerUrl = Objects.toString(model.get("oneClickUnsubscribeUrl"), unsubscribeUrl);
        if (marketing && (!isUnsubscribeUrl(unsubscribeUrl) || !isUnsubscribeUrl(headerUrl))) {
            throw new IllegalArgumentException("Marketing email needs an HTTPS unsubscribeUrl");
        }
        String html = templates.render(template, values);
        if (!enabled && !capture) {
            log.info("Email disabled (MAIL_ENABLED=false). Rendered '{}' for {} ({} characters)", template, to, html.length());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            String plainText = EmailTemplates.plainText(html) + "\n\n" + Objects.toString(values.get("actionUrl"), "");
            helper.setText(plainText, html);
            if (marketing) {
                message.setHeader("List-Unsubscribe", "<" + headerUrl + ">");
                if (model.containsKey("oneClickUnsubscribeUrl")) {
                    message.setHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
                }
            }
            if (marketing && !capture) {
                // FlowRunner owns marketing retries and consent checks. Propagate SMTP errors
                // so it does not advance a failed flow step or count it as sent.
                mailSender.send(message);
            } else {
                long validityMinutes = template.equals("password-reset")
                        ? ((Number) model.getOrDefault("resetMinutes", 60)).longValue() : 1440;
                outbox.enqueue(to, subject, message, LocalDateTime.now().plusMinutes(validityMinutes));
            }
        } catch (MessagingException e) {
            throw new IllegalStateException("Could not prepare email", e);
        }
    }

    private static boolean isUnsubscribeUrl(String url) {
        return (url.startsWith("https://") || url.matches("http://localhost(:\\d+)?/.*"))
                && !url.contains("\r") && !url.contains("\n");
    }
}
