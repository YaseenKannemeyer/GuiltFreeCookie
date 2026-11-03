package za.ac.cput.guiltfreecookie.service.flow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Cart;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.EmailFlow;
import za.ac.cput.guiltfreecookie.domain.EmailFlowSettings;
import za.ac.cput.guiltfreecookie.domain.EnrollmentStatus;
import za.ac.cput.guiltfreecookie.domain.FlowEnrollment;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.FlowEnrollmentRepository;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

@Service
public class FlowEnrollmentService {

    public static final ZoneId SHOP_ZONE = ZoneId.of("Africa/Johannesburg");
    static final LocalTime SEND_FROM = LocalTime.of(8, 0);
    static final LocalTime SEND_UNTIL = LocalTime.of(20, 0);
    static final Duration BROWSE_COOLDOWN = Duration.ofDays(7);

    private static final Set<EmailFlow> ENDED_BY_PURCHASE = EnumSet.of(
            EmailFlow.WELCOME, EmailFlow.BROWSE_ABANDONMENT, EmailFlow.CART_ABANDONMENT,
            EmailFlow.WINBACK, EmailFlow.REPLENISHMENT);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final FlowEnrollmentRepository repository;
    private final EmailFlowSettingsService settingsService;
    private final MarketingConsent consent;
    private final CartRepository cartRepository;

    public FlowEnrollmentService(FlowEnrollmentRepository repository, EmailFlowSettingsService settingsService,
                                 MarketingConsent consent, CartRepository cartRepository) {
        this.repository = repository;
        this.settingsService = settingsService;
        this.consent = consent;
        this.cartRepository = cartRepository;
    }

    // Once per address, ever: resubscribing doesn't send a second welcome code.
    @Transactional
    public void welcome(String email) {
        if (!repository.existsByEmailAndFlow(email, EmailFlow.WELCOME)) {
            enroll(email, EmailFlow.WELCOME, null);
        }
    }

    @Transactional
    public void browse(String email, String cookieId) {
        if (repository.existsByEmailAndFlowAndEnrolledAtAfter(email, EmailFlow.BROWSE_ABANDONMENT,
                LocalDateTime.now().minus(BROWSE_COOLDOWN))
                || repository.existsByEmailAndFlowAndStatus(email, EmailFlow.CART_ABANDONMENT, EnrollmentStatus.ACTIVE)) {
            return;
        }
        enroll(email, EmailFlow.BROWSE_ABANDONMENT, cookieId);
    }

    @Transactional
    public void abandonedCart(Cart cart) {
        if (repository.existsByFlowAndContextRef(EmailFlow.CART_ABANDONMENT, cart.getCartId())) {
            return;
        }
        if (cart.getRecoveryToken() == null) {
            cartRepository.save(new Cart.Builder().copy(cart).setRecoveryToken(randomToken()).build());
        }
        exit(cart.getCustomerEmail(), EnumSet.of(EmailFlow.BROWSE_ABANDONMENT), "Cart reminder took over");
        enroll(cart.getCustomerEmail(), EmailFlow.CART_ABANDONMENT, cart.getCartId());
    }

    @Transactional
    public void delivered(CustomerOrder order) {
        if (!repository.existsByFlowAndContextRef(EmailFlow.POST_PURCHASE, order.getOrderNumber())) {
            enroll(order.getCustomerEmail(), EmailFlow.POST_PURCHASE, order.getOrderNumber());
            enroll(order.getCustomerEmail(), EmailFlow.REPLENISHMENT, order.getOrderNumber());
        }
    }

    @Transactional
    public void winback(String email, LocalDateTime lastPaidAt) {
        if (!repository.existsByEmailAndFlowAndEnrolledAtAfter(email, EmailFlow.WINBACK, lastPaidAt)) {
            enroll(email, EmailFlow.WINBACK, null);
        }
    }

    @Transactional
    public void sunset(String email, LocalDateTime notSince) {
        if (!repository.existsByEmailAndFlowAndEnrolledAtAfter(email, EmailFlow.SUNSET, notSince)) {
            enroll(email, EmailFlow.SUNSET, null);
        }
    }

    @Transactional
    // The first welcome email (with the code they signed up for) still goes out; the rest of the series stops.
    public void purchased(String email) {
        for (FlowEnrollment enrollment : repository.findByEmailAndFlowInAndStatus(email, ENDED_BY_PURCHASE, EnrollmentStatus.ACTIVE)) {
            if (enrollment.getFlow() == EmailFlow.WELCOME && enrollment.getCurrentStep() == 0) {
                continue;
            }
            repository.save(new FlowEnrollment.Builder().copy(enrollment)
                    .setStatus(EnrollmentStatus.EXITED).setExitReason("Purchased").setUpdatedAt(LocalDateTime.now()).build());
        }
    }

    private void enroll(String email, EmailFlow flow, String contextRef) {
        EmailFlowSettings settings = settingsService.get();
        if (!settings.isEnabled(flow) || !consent.canSend(email, flow)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Duration first = FlowCatalog.delays(flow, settings).getFirst();
        repository.save(new FlowEnrollment.Builder()
                .setEmail(email)
                .setFlow(flow)
                .setContextRef(contextRef)
                .setCurrentStep(0)
                .setNextSendAt(sendTime(now.plus(first), flow == EmailFlow.WELCOME))
                .setStatus(EnrollmentStatus.ACTIVE)
                .setEnrolledAt(now)
                .setUpdatedAt(now)
                .build());
    }

    private void exit(String email, Set<EmailFlow> flows, String reason) {
        for (FlowEnrollment enrollment : repository.findByEmailAndFlowInAndStatus(email, flows, EnrollmentStatus.ACTIVE)) {
            repository.save(new FlowEnrollment.Builder().copy(enrollment)
                    .setStatus(EnrollmentStatus.EXITED).setExitReason(reason).setUpdatedAt(LocalDateTime.now()).build());
        }
    }

    // The welcome email (which they just asked for) goes now; everything else waits for 08:00–20:00 shop time.
    static LocalDateTime sendTime(LocalDateTime due, boolean immediate) {
        if (immediate) {
            return due;
        }
        ZonedDateTime local = due.atZone(ZoneId.systemDefault()).withZoneSameInstant(SHOP_ZONE);
        if (local.toLocalTime().isBefore(SEND_FROM)) {
            local = local.with(SEND_FROM);
        } else if (!local.toLocalTime().isBefore(SEND_UNTIL)) {
            local = local.plusDays(1).with(SEND_FROM);
        }
        return local.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    static String randomToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
