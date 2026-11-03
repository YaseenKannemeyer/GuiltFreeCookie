package za.ac.cput.guiltfreecookie.service.flow;

import org.springframework.stereotype.Component;
import za.ac.cput.guiltfreecookie.service.notification.EmailService;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Someone who unsubscribed only comes back by clicking a link sent to their own inbox, so nobody can
// sign another person back up. At most one of these a day per address.
@Component
public class ResubscribeConfirmation {

    static final Duration RESEND_AFTER = Duration.ofHours(24);

    private final EmailService emailService;
    private final MarketingLinks links;
    private final Map<String, Instant> lastSent = new ConcurrentHashMap<>();

    public ResubscribeConfirmation(EmailService emailService, MarketingLinks links) {
        this.emailService = emailService;
        this.links = links;
    }

    public void send(String email) {
        Instant now = Instant.now();
        Instant previous = lastSent.get(email);
        if (previous != null && previous.isAfter(now.minus(RESEND_AFTER))) {
            return;
        }
        lastSent.put(email, now);
        lastSent.values().removeIf(sent -> sent.isBefore(now.minus(RESEND_AFTER)));
        emailService.sendTemplate(email, "Confirm you'd like our emails again", "resubscribe-confirm",
                Map.of("actionUrl", links.frontend("/email/resubscribe?e=" + MarketingLinks.encode(email) + "&t=" + links.token(email)),
                        "actionLabel", "Yes, sign me up again"), false);
    }
}
