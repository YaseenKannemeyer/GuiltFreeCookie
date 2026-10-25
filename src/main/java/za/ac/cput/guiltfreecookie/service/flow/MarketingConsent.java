package za.ac.cput.guiltfreecookie.service.flow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.EmailFlow;
import za.ac.cput.guiltfreecookie.domain.EmailSuppression;
import za.ac.cput.guiltfreecookie.domain.EnrollmentStatus;
import za.ac.cput.guiltfreecookie.domain.FlowEnrollment;
import za.ac.cput.guiltfreecookie.domain.NewsletterSubscriber;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.SubscriberStatus;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.EmailSuppressionRepository;
import za.ac.cput.guiltfreecookie.repository.FlowEnrollmentRepository;
import za.ac.cput.guiltfreecookie.repository.NewsletterSubscriberRepository;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Who may get which flow (POPIA s69). Newsletter flows need an active opt-in. Cart and browse
 * reminders need an opt-in or an existing customer, because a guest who only typed their email
 * at checkout hasn't agreed to marketing yet. Post-purchase and replenishment rely on the
 * existing-customer rule. An unsubscribe or a pause stops all of them.
 */
@Service
public class MarketingConsent {

    private final NewsletterSubscriberRepository subscriberRepository;
    private final EmailSuppressionRepository suppressionRepository;
    private final CustomerOrderRepository orderRepository;
    private final FlowEnrollmentRepository enrollmentRepository;

    public MarketingConsent(NewsletterSubscriberRepository subscriberRepository,
                            EmailSuppressionRepository suppressionRepository,
                            CustomerOrderRepository orderRepository,
                            FlowEnrollmentRepository enrollmentRepository) {
        this.subscriberRepository = subscriberRepository;
        this.suppressionRepository = suppressionRepository;
        this.orderRepository = orderRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public boolean canSend(String email, EmailFlow flow) {
        if (suppressionRepository.existsById(email)) {
            return false;
        }
        SubscriberStatus status = subscriberRepository.findById(email).map(NewsletterSubscriber::getStatus).orElse(null);
        if (status == SubscriberStatus.UNSUBSCRIBED || status == SubscriberStatus.PAUSED) {
            return false;
        }
        boolean subscribed = status == SubscriberStatus.ACTIVE;
        return switch (flow) {
            case WELCOME, WINBACK, SUNSET -> subscribed;
            case BROWSE_ABANDONMENT, CART_ABANDONMENT -> subscribed || isCustomer(email);
            case POST_PURCHASE, REPLENISHMENT -> true;
        };
    }

    public boolean isCustomer(String email) {
        return orderRepository.existsByCustomerEmailAndStatusIn(email, OrderStatus.PAID_STATUSES);
    }

    @Transactional
    public void unsubscribe(String email, String reason) {
        if (!suppressionRepository.existsById(email)) {
            suppressionRepository.save(new EmailSuppression.Builder()
                    .setEmail(email).setReason(reason).setCreatedAt(LocalDateTime.now()).build());
        }
        subscriberRepository.findById(email).ifPresent(subscriber -> setStatus(subscriber, SubscriberStatus.UNSUBSCRIBED));
        exitAll(email, "Unsubscribed");
    }

    // Undoes an unsubscribe. Only called on an explicit request from the person.
    @Transactional
    public void resubscribe(String email, String source) {
        suppressionRepository.deleteById(email);
        subscriberRepository.findById(email).ifPresentOrElse(
                subscriber -> setStatus(subscriber, SubscriberStatus.ACTIVE),
                () -> subscriberRepository.save(new NewsletterSubscriber.Builder()
                        .setEmail(email).setSubscribedAt(Instant.now()).setSource(source)
                        .setStatusChangedAt(Instant.now()).build()));
    }

    @Transactional
    public void pause(String email) {
        subscriberRepository.findById(email).ifPresent(subscriber -> setStatus(subscriber, SubscriberStatus.PAUSED));
    }

    @Transactional
    public void markEngaged(String email) {
        subscriberRepository.findById(email).ifPresent(subscriber -> subscriberRepository.save(
                new NewsletterSubscriber.Builder().copy(subscriber).setLastEngagedAt(Instant.now()).build()));
    }

    public boolean isUnsubscribed(String email) {
        return suppressionRepository.existsById(email)
                || subscriberRepository.findById(email).map(s -> s.getStatus() == SubscriberStatus.UNSUBSCRIBED).orElse(false);
    }

    private void setStatus(NewsletterSubscriber subscriber, SubscriberStatus status) {
        subscriberRepository.save(new NewsletterSubscriber.Builder().copy(subscriber)
                .setStatus(status).setStatusChangedAt(Instant.now()).build());
    }

    private void exitAll(String email, String reason) {
        for (FlowEnrollment enrollment : enrollmentRepository.findByEmailAndStatus(email, EnrollmentStatus.ACTIVE)) {
            enrollmentRepository.save(new FlowEnrollment.Builder().copy(enrollment)
                    .setStatus(EnrollmentStatus.EXITED).setExitReason(reason).setUpdatedAt(LocalDateTime.now()).build());
        }
    }
}
