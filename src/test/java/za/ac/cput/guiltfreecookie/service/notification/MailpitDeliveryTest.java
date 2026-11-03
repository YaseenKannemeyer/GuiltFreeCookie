package za.ac.cput.guiltfreecookie.service.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import za.ac.cput.guiltfreecookie.domain.EmailDelivery;
import za.ac.cput.guiltfreecookie.repository.EmailDeliveryRepository;

// Opt-in: RUN_MAILPIT_TEST=true with Mailpit listening on 127.0.0.1:1025. View at http://127.0.0.1:8025.
@SpringBootTest(properties = {"app.mail.enabled=true", "spring.mail.host=127.0.0.1", "spring.mail.port=1025"})
@EnabledIfEnvironmentVariable(named = "RUN_MAILPIT_TEST", matches = "true")
class MailpitDeliveryTest {

    @Autowired
    private EmailTemplates templates;

    @Autowired private EmailService service;
    @Autowired private EmailOutbox outbox;
    @Autowired private EmailDeliveryRepository deliveries;

    @Test
    void everyTemplateReachesTheLocalInbox() {
        for (String template : EmailTemplates.NAMES) {
            service.sendTemplate("preview@example.test", "[Preview] " + template, template, templates.sample(template), false);
        }
        // Drain more than one batch: the studio has more than 25 templates.
        for (int batch = 0; batch < 4; batch++) outbox.deliverDue();
        assertEquals(EmailTemplates.NAMES.size(), deliveries.findAll().stream()
                .filter(d -> d.getSubject().startsWith("[Preview]")).count());
        assertTrue(deliveries.findAll().stream().filter(d -> d.getSubject().startsWith("[Preview]"))
                .allMatch(d -> d.getStatus() == EmailDelivery.Status.SENT));
    }
}
