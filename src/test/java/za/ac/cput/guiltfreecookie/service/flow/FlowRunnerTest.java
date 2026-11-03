package za.ac.cput.guiltfreecookie.service.flow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import za.ac.cput.guiltfreecookie.domain.EmailFlow;
import za.ac.cput.guiltfreecookie.domain.EmailSend;
import za.ac.cput.guiltfreecookie.domain.EnrollmentStatus;
import za.ac.cput.guiltfreecookie.domain.FlowEnrollment;
import za.ac.cput.guiltfreecookie.repository.EmailSendRepository;
import za.ac.cput.guiltfreecookie.repository.FlowEnrollmentRepository;
import za.ac.cput.guiltfreecookie.service.notification.EmailService;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FlowRunnerTest {

    private static final String EMAIL = "thandi@example.co.za";
    private static final FlowData DATA = new FlowData(List.of(), List.of(), List.of(), List.of(),
            null, null, 10, "Lemon Almond", "https://shop.example/cart", 21);

    @Mock
    private FlowEnrollmentRepository enrollmentRepository;
    @Mock
    private EmailSendRepository sendRepository;
    @Mock
    private EmailFlowSettingsService settingsService;
    @Mock
    private MarketingConsent consent;
    @Mock
    private FlowDataLoader loader;
    @Mock
    private MarketingLinks links;
    @Mock
    private EmailService emailService;
    @Mock
    private TransactionTemplate transactions;

    @InjectMocks
    private FlowRunner runner;

    @BeforeEach
    void setUp() {
        when(settingsService.get()).thenReturn(FlowEnrollmentServiceTest.settings(true));
        when(consent.canSend(any(), any())).thenReturn(true);
        when(links.frontend(anyString())).thenAnswer(call -> "https://shop.example" + call.getArgument(0));
        when(links.tracked(anyString(), anyString())).thenAnswer(call -> call.getArgument(1));
        when(links.unsubscribePage(EMAIL)).thenReturn("https://shop.example/email/unsubscribe?e=x&t=y");
        when(links.oneClickUnsubscribe(EMAIL)).thenReturn("https://api.example/one-click");
        when(links.openPixel(anyString())).thenReturn("https://api.example/o.gif");
    }

    @Test
    void sendsTheStepKeepsTheCodeAndSchedulesTheNextOne() {
        FlowEnrollment welcome = enrollment(EmailFlow.WELCOME, 0);
        when(loader.load(any(), eq(0), any())).thenReturn(new FlowDataLoader.Result(DATA, null, false, "WELCOME-ABC234"));

        assertTrue(runner.process(welcome));

        verify(emailService).sendTemplate(eq(EMAIL), anyString(), eq("marketing-flow"),
                argThat(model -> model.get("unsubscribeUrl") != null && model.get("pixelUrl") != null
                        && model.get("oneClickUnsubscribeUrl") != null), eq(true));
        verify(sendRepository).save(argThat((EmailSend send) -> send.getSendId().length() == 32 && send.getStep() == 0));
        verify(enrollmentRepository).save(argThat(e -> e.getCurrentStep() == 1 && e.getStatus() == EnrollmentStatus.ACTIVE
                && "WELCOME-ABC234".equals(e.getDiscountCode())
                && !e.getNextSendAt().isBefore(welcome.getEnrolledAt().plusHours(24))));
    }

    @Test
    void noConsentEndsTheFlowWithoutSending() {
        when(consent.canSend(EMAIL, EmailFlow.CART_ABANDONMENT)).thenReturn(false);

        assertFalse(runner.process(enrollment(EmailFlow.CART_ABANDONMENT, 0)));

        verify(emailService, never()).sendTemplate(anyString(), anyString(), anyString(), anyMap(), anyBoolean());
        verify(enrollmentRepository).save(argThat(e -> e.getStatus() == EnrollmentStatus.EXITED));
    }

    @Test
    void waitsIfAnotherFlowEmailWentOutRecently() {
        when(sendRepository.existsByEmailAndSentAtAfter(eq(EMAIL), any())).thenReturn(true);

        assertFalse(runner.process(enrollment(EmailFlow.CART_ABANDONMENT, 1)));

        verify(loader, never()).load(any(), any(Integer.class), any());
        verify(enrollmentRepository, never()).save(argThat(e -> e.getStatus() != EnrollmentStatus.ACTIVE));
    }

    @Test
    void anExitFromTheLoaderEndsTheFlow() {
        when(loader.load(any(), eq(1), any())).thenReturn(FlowDataLoader.Result.exit("Purchased"));

        assertFalse(runner.process(enrollment(EmailFlow.WELCOME, 1)));

        verify(enrollmentRepository).save(argThat(e -> e.getStatus() == EnrollmentStatus.EXITED && "Purchased".equals(e.getExitReason())));
    }

    @Test
    void aSkippedLastStepCompletesWithoutSending() {
        when(loader.load(any(), eq(4), any())).thenReturn(FlowDataLoader.Result.skipStep());

        assertFalse(runner.process(enrollment(EmailFlow.WELCOME, 4)));

        verify(emailService, never()).sendTemplate(anyString(), anyString(), anyString(), anyMap(), anyBoolean());
        verify(enrollmentRepository).save(argThat(e -> e.getStatus() == EnrollmentStatus.COMPLETED));
    }

    @Test
    void sunsetPausesSomeoneWhoNeverClicked() {
        FlowEnrollment finished = enrollment(EmailFlow.SUNSET, 3);
        when(loader.engagedSince(finished)).thenReturn(false);

        runner.process(finished);

        verify(consent).pause(EMAIL);
        verify(enrollmentRepository).save(argThat(e -> e.getStatus() == EnrollmentStatus.COMPLETED));
    }

    @Test
    void aSwitchedOffFlowStops() {
        when(settingsService.get()).thenReturn(FlowEnrollmentServiceTest.settings(false));

        runner.process(enrollment(EmailFlow.WINBACK, 0));

        verify(enrollmentRepository).save(argThat(e -> e.getStatus() == EnrollmentStatus.EXITED));
        verify(emailService, never()).sendTemplate(anyString(), anyString(), anyString(), anyMap(), anyBoolean());
    }

    private static FlowEnrollment enrollment(EmailFlow flow, int step) {
        return new FlowEnrollment.Builder().setEnrollmentId("e1").setEmail(EMAIL).setFlow(flow).setCurrentStep(step)
                .setStatus(EnrollmentStatus.ACTIVE).setEnrolledAt(LocalDateTime.now().minusMinutes(1))
                .setNextSendAt(LocalDateTime.now()).build();
    }
}
