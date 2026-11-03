package za.ac.cput.guiltfreecookie.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;
import za.ac.cput.guiltfreecookie.domain.EmailDelivery;
import za.ac.cput.guiltfreecookie.repository.EmailDeliveryRepository;
import java.time.LocalDateTime;

@Component("outboxHealthIndicator")
public class OutboxHealthIndicator extends AbstractHealthIndicator {
    private final EmailDeliveryRepository repository;
    private final boolean enabled;
    public OutboxHealthIndicator(EmailDeliveryRepository repository, @Value("${app.mail.enabled}") boolean enabled) {
        this.repository = repository;
        this.enabled = enabled;
    }
    @Override
    protected void doHealthCheck(Health.Builder builder) {
        if (!enabled) { builder.up(); return; } // captured demo emails are intentionally not delivered
        boolean failed = repository.countByStatus(EmailDelivery.Status.FAILED) > 0;
        boolean stalled = repository.existsByStatusAndNextAttemptAtBefore(EmailDelivery.Status.PENDING, LocalDateTime.now().minusMinutes(30));
        if (failed || stalled) builder.down(); else builder.up();
    }
}
