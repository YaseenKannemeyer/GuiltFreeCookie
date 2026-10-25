package za.ac.cput.guiltfreecookie.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.NewsletterSubscriber;
import za.ac.cput.guiltfreecookie.domain.SubscriberStatus;
import za.ac.cput.guiltfreecookie.repository.NewsletterSubscriberRepository;
import za.ac.cput.guiltfreecookie.service.flow.FlowEnrollmentService;
import za.ac.cput.guiltfreecookie.service.flow.MarketingConsent;
import za.ac.cput.guiltfreecookie.service.flow.ResubscribeConfirmation;

import java.time.Instant;
import java.util.List;

@Service
public class NewsletterService {

    private final NewsletterSubscriberRepository repository;
    private final MarketingConsent consent;
    private final FlowEnrollmentService flows;
    private final ResubscribeConfirmation confirmation;

    public NewsletterService(NewsletterSubscriberRepository repository, MarketingConsent consent,
                             FlowEnrollmentService flows, ResubscribeConfirmation confirmation) {
        this.repository = repository;
        this.consent = consent;
        this.flows = flows;
        this.confirmation = confirmation;
    }

    public void subscribe(String email) {
        subscribe(email, null);
    }

    /**
     * Idempotent: subscribing twice keeps the original opt-in date. An address that unsubscribed or was
     * paused gets a confirmation link instead, since anyone can type anyone's address into the form.
     * The welcome emails go to each address only once.
     */
    @Transactional
    public void subscribe(String email, String source) {
        NewsletterSubscriber existing = repository.findById(email).orElse(null);
        if (consent.isUnsubscribed(email) || (existing != null && existing.getStatus() != SubscriberStatus.ACTIVE)) {
            confirmation.send(email);
            return;
        }
        if (existing == null) {
            repository.save(new NewsletterSubscriber.Builder()
                    .setEmail(email).setSubscribedAt(Instant.now()).setSource(source).setStatusChangedAt(Instant.now()).build());
        }
        flows.welcome(email);
    }

    public List<NewsletterSubscriber> getAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "subscribedAt"));
    }
}
