package za.ac.cput.guiltfreecookie.service.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSenderImpl;

// Opt-in: RUN_MAILPIT_TEST=true with Mailpit listening on 127.0.0.1:1025. View at http://127.0.0.1:8025.
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_MAILPIT_TEST", matches = "true")
class MailpitDeliveryTest {

    @Autowired
    private EmailTemplates templates;

    @Test
    void everyTemplateReachesTheLocalInbox() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("127.0.0.1");
        sender.setPort(1025);
        EmailService service = new EmailService(sender, true, "bakery@example.test", templates);

        for (String template : EmailTemplates.NAMES) {
            service.sendTemplate("preview@example.test", "[Preview] " + template, template, templates.sample(template), false);
        }
    }
}
