package za.ac.cput.guiltfreecookie.service.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_MAILPIT_TEST", matches = "true")
class MailpitDeliveryTest {

    @Autowired
    EmailTemplates templates;

    @Test
    void allSixTemplatesReachTheLocalCaptureInbox() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("127.0.0.1");
        sender.setPort(1025);
        EmailService service = new EmailService(
            sender,
            true,
            "bakery@example.test",
            templates
        );
        for (String template : EmailTemplates.NAMES) {
            service.sendTemplate(
                "preview@example.test",
                "[Phase 6 QA] " + template,
                template,
                templates.sample(template),
                false
            );
        }
    }
}
