package za.ac.cput.guiltfreecookie.service.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import za.ac.cput.guiltfreecookie.domain.EmailFlow;
import za.ac.cput.guiltfreecookie.domain.EmailFlowSettings;
import za.ac.cput.guiltfreecookie.domain.EmailSend;
import za.ac.cput.guiltfreecookie.domain.EnrollmentStatus;
import za.ac.cput.guiltfreecookie.domain.FlowEnrollment;
import za.ac.cput.guiltfreecookie.repository.EmailSendRepository;
import za.ac.cput.guiltfreecookie.repository.FlowEnrollmentRepository;
import za.ac.cput.guiltfreecookie.service.notification.EmailService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Sends whatever flow steps are due. Each enrolment is handled in its own transaction.
@Service
public class FlowRunner {

    private static final Logger log = LoggerFactory.getLogger(FlowRunner.class);
    static final int BATCH = 100;
    // At most one flow email per address in this window, so two flows never land on the same day.
    static final Duration QUIET_GAP = Duration.ofHours(16);
    static final Duration RETRY_AFTER = Duration.ofHours(4);

    private final FlowEnrollmentRepository enrollmentRepository;
    private final EmailSendRepository sendRepository;
    private final EmailFlowSettingsService settingsService;
    private final MarketingConsent consent;
    private final FlowDataLoader loader;
    private final MarketingLinks links;
    private final EmailService emailService;
    private final TransactionTemplate transactions;

    public FlowRunner(FlowEnrollmentRepository enrollmentRepository, EmailSendRepository sendRepository,
                      EmailFlowSettingsService settingsService, MarketingConsent consent, FlowDataLoader loader,
                      MarketingLinks links, EmailService emailService, TransactionTemplate transactions) {
        this.enrollmentRepository = enrollmentRepository;
        this.sendRepository = sendRepository;
        this.settingsService = settingsService;
        this.consent = consent;
        this.loader = loader;
        this.links = links;
        this.emailService = emailService;
        this.transactions = transactions;
    }

    public int runDue() {
        List<FlowEnrollment> due = enrollmentRepository.findByStatusAndNextSendAtLessThanEqualOrderByNextSendAtAsc(
                EnrollmentStatus.ACTIVE, LocalDateTime.now(), PageRequest.of(0, BATCH));
        int sent = 0;
        for (FlowEnrollment enrollment : due) {
            try {
                Boolean wasSent = transactions.execute(status -> process(enrollmentRepository
                        .findById(enrollment.getEnrollmentId()).orElse(enrollment)));
                if (Boolean.TRUE.equals(wasSent)) {
                    sent++;
                }
            } catch (RuntimeException e) {
                log.error("Flow step failed for enrolment {} ({})", enrollment.getEnrollmentId(), enrollment.getFlow(), e);
                transactions.executeWithoutResult(status -> retryLater(enrollment));
            }
        }
        return sent;
    }

    // Returns true when an email went out.
    boolean process(FlowEnrollment enrollment) {
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            return false;
        }
        EmailFlowSettings settings = settingsService.get();
        EmailFlow flow = enrollment.getFlow();
        if (!settings.isEnabled(flow)) {
            end(enrollment, EnrollmentStatus.EXITED, "Flow switched off");
            return false;
        }
        List<Duration> delays = FlowCatalog.delays(flow, settings);
        int step = enrollment.getCurrentStep();
        if (step >= delays.size()) {
            finish(enrollment);
            return false;
        }
        if (!consent.canSend(enrollment.getEmail(), flow)) {
            end(enrollment, EnrollmentStatus.EXITED, "No marketing consent");
            return false;
        }
        boolean firstWelcome = flow == EmailFlow.WELCOME && step == 0;
        if (!firstWelcome && sendRepository.existsByEmailAndSentAtAfter(enrollment.getEmail(), LocalDateTime.now().minus(QUIET_GAP))) {
            retryLater(enrollment);
            return false;
        }

        FlowDataLoader.Result result = loader.load(enrollment, step, settings);
        if (result.exitReason() != null) {
            end(enrollment, EnrollmentStatus.EXITED, result.exitReason());
            return false;
        }
        String code = result.issuedCode() != null ? result.issuedCode() : enrollment.getDiscountCode();
        if (!result.skip()) {
            send(enrollment, step, result.data());
        }
        advance(new FlowEnrollment.Builder().copy(enrollment).setDiscountCode(code).build(), delays);
        return !result.skip();
    }

    private void send(FlowEnrollment enrollment, int step, FlowData data) {
        String email = enrollment.getEmail();
        String sendId = FlowEnrollmentService.randomToken().substring(0, 32);
        FlowCopy.Message message = FlowCopy.compose(enrollment.getFlow(), step, data, links.frontend(""),
                url -> links.tracked(sendId, url));
        sendRepository.save(new EmailSend.Builder()
                .setSendId(sendId)
                .setEmail(email)
                .setFlow(enrollment.getFlow())
                .setStep(step)
                .setContextRef(enrollment.getContextRef())
                .setSentAt(LocalDateTime.now())
                .build());
        Map<String, Object> model = new HashMap<>(message.model());
        model.put("unsubscribeUrl", links.unsubscribePage(email));
        model.put("oneClickUnsubscribeUrl", links.oneClickUnsubscribe(email));
        model.put("pixelUrl", links.openPixel(sendId));
        emailService.sendTemplate(email, message.subject(), "marketing-flow", model, true);
    }

    private void advance(FlowEnrollment enrollment, List<Duration> delays) {
        int next = enrollment.getCurrentStep() + 1;
        LocalDateTime now = LocalDateTime.now();
        FlowEnrollment.Builder updated = new FlowEnrollment.Builder().copy(enrollment).setCurrentStep(next).setUpdatedAt(now);
        if (next < delays.size()) {
            LocalDateTime due = enrollment.getEnrolledAt().plus(delays.get(next));
            updated.setNextSendAt(FlowEnrollmentService.sendTime(due.isAfter(now) ? due : now.plusHours(1), false));
        } else if (enrollment.getFlow() == EmailFlow.SUNSET) {
            updated.setNextSendAt(now.plus(FlowCatalog.SUNSET_GRACE));
        } else {
            updated.setNextSendAt(null).setStatus(EnrollmentStatus.COMPLETED);
        }
        enrollmentRepository.save(updated.build());
    }

    // Only sunset has anything to do after its last email: pause anyone who never clicked.
    private void finish(FlowEnrollment enrollment) {
        if (enrollment.getFlow() == EmailFlow.SUNSET && !loader.engagedSince(enrollment)) {
            consent.pause(enrollment.getEmail());
            end(enrollment, EnrollmentStatus.COMPLETED, "Paused after no engagement");
            return;
        }
        end(enrollment, EnrollmentStatus.COMPLETED, null);
    }

    private void retryLater(FlowEnrollment enrollment) {
        enrollmentRepository.findById(enrollment.getEnrollmentId()).ifPresent(current -> enrollmentRepository.save(
                new FlowEnrollment.Builder().copy(current)
                        .setNextSendAt(FlowEnrollmentService.sendTime(LocalDateTime.now().plus(RETRY_AFTER), false))
                        .setUpdatedAt(LocalDateTime.now()).build()));
    }

    private void end(FlowEnrollment enrollment, EnrollmentStatus status, String reason) {
        enrollmentRepository.save(new FlowEnrollment.Builder().copy(enrollment)
                .setStatus(status).setExitReason(reason).setNextSendAt(null).setUpdatedAt(LocalDateTime.now()).build());
    }
}
