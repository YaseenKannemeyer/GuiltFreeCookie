package za.ac.cput.guiltfreecookie.service.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import za.ac.cput.guiltfreecookie.domain.Cart;
import za.ac.cput.guiltfreecookie.domain.EmailFlowSettings;
import za.ac.cput.guiltfreecookie.domain.NewsletterSubscriber;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.repository.CartItemRepository;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.NewsletterSubscriberRepository;
import za.ac.cput.guiltfreecookie.service.order.OrderEvents;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

// What starts and stops flows: order events, idle carts, and a daily look for lapsed customers and quiet subscribers.
@Component
public class FlowTriggers {

    private static final Logger log = LoggerFactory.getLogger(FlowTriggers.class);
    static final Duration CART_IDLE = Duration.ofHours(1);
    static final Duration CART_TOO_OLD = Duration.ofDays(7);
    // Winback only looks this far past the cutoff, so a long-gone customer isn't emailed out of the blue.
    static final Duration WINBACK_WINDOW = Duration.ofDays(30);

    private final FlowEnrollmentService enrollments;
    private final FlowRunner runner;
    private final EmailFlowSettingsService settingsService;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerOrderRepository orderRepository;
    private final NewsletterSubscriberRepository subscriberRepository;

    public FlowTriggers(FlowEnrollmentService enrollments, FlowRunner runner, EmailFlowSettingsService settingsService,
                        CartRepository cartRepository, CartItemRepository cartItemRepository,
                        CustomerOrderRepository orderRepository, NewsletterSubscriberRepository subscriberRepository) {
        this.enrollments = enrollments;
        this.runner = runner;
        this.settingsService = settingsService;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.subscriberRepository = subscriberRepository;
    }

    @Scheduled(fixedDelayString = "${app.flows.run-ms}", initialDelayString = "${app.flows.run-ms}")
    public void sendDue() {
        int sent = runner.runDue();
        if (sent > 0) {
            log.info("Sent {} flow email(s)", sent);
        }
    }

    @Scheduled(fixedDelayString = "${app.flows.cart-check-ms}", initialDelayString = "${app.flows.cart-check-ms}")
    public void findAbandonedCarts() {
        LocalDateTime now = LocalDateTime.now();
        for (Cart cart : cartRepository.findIdleWithEmail(now.minus(CART_IDLE), now.minus(CART_TOO_OLD))) {
            if (cartItemRepository.countByCartId(cart.getCartId()) > 0) {
                enrollments.abandonedCart(cart);
            }
        }
    }

    @Scheduled(cron = "${app.flows.daily-cron}", zone = "Africa/Johannesburg")
    public void daily() {
        EmailFlowSettings settings = settingsService.get();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lapsed = now.minusDays(settings.getWinbackDays());
        for (Object[] row : orderRepository.lastPaidBetween(OrderStatus.PAID_STATUSES, lapsed.minus(WINBACK_WINDOW), lapsed)) {
            enrollments.winback((String) row[0], (LocalDateTime) row[1]);
        }
        Instant quietSince = Instant.now().minus(Duration.ofDays(settings.getSunsetDays()));
        for (NewsletterSubscriber subscriber : subscriberRepository.findDisengaged(quietSince)) {
            enrollments.sunset(subscriber.getEmail(), LocalDateTime.ofInstant(quietSince, ZoneId.systemDefault()));
        }
    }

    // After the order's own transaction has committed, so these writes need a fresh one.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaid(OrderEvents.Paid event) {
        orderRepository.findByOrderNumber(event.orderNumber())
                .ifPresent(order -> enrollments.purchased(order.getCustomerEmail()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDelivered(OrderEvents.Delivered event) {
        orderRepository.findByOrderNumber(event.orderNumber()).ifPresent(enrollments::delivered);
    }
}
