package za.ac.cput.guiltfreecookie.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
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

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.enabled}") boolean enabled,
            @Value("${app.mail.from}") String from,
            EmailTemplates templates) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.templates = templates;
    }

    @Async
    public void send(String to, String subject, String body) {
        if (!enabled) {
            log.info("Email disabled (MAIL_ENABLED=false). Would send '{}' to {}", subject, to);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send email '{}' to {}", subject, to, e);
        }
    }

    // Branded HTML plus a plain-text alternative. Marketing email must carry an HTTPS unsubscribe
    // link, which also goes in the List-Unsubscribe header. Bodies are never logged (they can hold reset links).
    @Async
    public void sendTemplate(String to, String subject, String template, Map<String, Object> model, boolean marketing) {
        Map<String, Object> values = new HashMap<>(model);
        values.put("marketing", marketing);
        String unsubscribeUrl = Objects.toString(model.get("unsubscribeUrl"), "");
        if (marketing && (!unsubscribeUrl.startsWith("https://")
                || unsubscribeUrl.contains("\r") || unsubscribeUrl.contains("\n"))) {
            throw new IllegalArgumentException("Marketing email needs an HTTPS unsubscribeUrl");
        }
        String html = templates.render(template, values);
        if (!enabled) {
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
                message.setHeader("List-Unsubscribe", "<" + unsubscribeUrl + ">");
            }
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send email template '{}' to {}", template, to, e);
        }
    }
}
