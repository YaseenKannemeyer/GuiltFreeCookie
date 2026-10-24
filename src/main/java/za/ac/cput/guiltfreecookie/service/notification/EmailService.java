package za.ac.cput.guiltfreecookie.service.notification;

import jakarta.mail.MessagingException;
import java.util.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.*;
import org.springframework.mail.javamail.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(
        EmailService.class
    );
    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;
    private final EmailTemplates templates;

    public EmailService(
        JavaMailSender mailSender,
        @Value("${app.mail.enabled}") boolean enabled,
        @Value("${app.mail.from}") String from,
        EmailTemplates templates
    ) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.templates = templates;
    }

    @Async
    public void send(String to, String subject, String body) {
        if (!enabled) {
            log.info("Email disabled. Would send '{}' to {}", subject, to);
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
            log.error("Failed to send '{}'", subject, e);
        }
    }

    @Async
    public void sendTemplate(
        String to,
        String subject,
        String template,
        Map<String, Object> model,
        boolean marketing
    ) {
        Map<String, Object> values = new HashMap<>(model);
        values.put("marketing", marketing);
        String unsubscribe = Objects.toString(model.get("unsubscribeUrl"), "");
        if (
            marketing &&
            (!unsubscribe.startsWith("https://") ||
                unsubscribe.contains("\r") ||
                unsubscribe.contains("\n"))
        ) throw new IllegalArgumentException(
            "Marketing email needs an HTTPS unsubscribeUrl"
        );
        String html = templates.render(template, values);
        if (!enabled) {
            log.info(
                "Email disabled. Rendered '{}' to {} ({} characters)",
                template,
                to,
                html.length()
            );
            return;
        }
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                message,
                true,
                "UTF-8"
            );
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(
                EmailTemplates.plainText(html) +
                    "\n\n" +
                    Objects.toString(values.get("actionUrl"), ""),
                html
            );
            if (marketing) message.setHeader(
                "List-Unsubscribe",
                "<" + unsubscribe + ">"
            );
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send template '{}'", template, e);
        }
    }
}
