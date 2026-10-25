package za.ac.cput.guiltfreecookie.service.flow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import za.ac.cput.guiltfreecookie.domain.EmailFlow;
import za.ac.cput.guiltfreecookie.domain.EmailSuppression;
import za.ac.cput.guiltfreecookie.domain.EnrollmentStatus;
import za.ac.cput.guiltfreecookie.domain.FlowEnrollment;
import za.ac.cput.guiltfreecookie.domain.NewsletterSubscriber;
import za.ac.cput.guiltfreecookie.domain.SubscriberStatus;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.EmailSuppressionRepository;
import za.ac.cput.guiltfreecookie.repository.FlowEnrollmentRepository;
import za.ac.cput.guiltfreecookie.repository.NewsletterSubscriberRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarketingConsentTest {

    private static final String EMAIL = "thandi@example.co.za";

    @Mock
    private NewsletterSubscriberRepository subscriberRepository;
    @Mock
    private EmailSuppressionRepository suppressionRepository;
    @Mock
    private CustomerOrderRepository orderRepository;
    @Mock
    private FlowEnrollmentRepository enrollmentRepository;

    @InjectMocks
    private MarketingConsent consent;

    @Test
    void newsletterFlowsNeedAnActiveSubscriber() {
        when(subscriberRepository.findById(EMAIL)).thenReturn(Optional.empty());
        assertFalse(consent.canSend(EMAIL, EmailFlow.WELCOME));
        assertFalse(consent.canSend(EMAIL, EmailFlow.WINBACK));

        subscribed(SubscriberStatus.ACTIVE);
        assertTrue(consent.canSend(EMAIL, EmailFlow.WELCOME));
        assertTrue(consent.canSend(EMAIL, EmailFlow.SUNSET));
    }

    @Test
    void cartRemindersNeedAnOptInOrAPastOrder() {
        when(subscriberRepository.findById(EMAIL)).thenReturn(Optional.empty());
        when(orderRepository.existsByCustomerEmailAndStatusIn(eq(EMAIL), any())).thenReturn(false);
        assertFalse(consent.canSend(EMAIL, EmailFlow.CART_ABANDONMENT));
        assertFalse(consent.canSend(EMAIL, EmailFlow.BROWSE_ABANDONMENT));

        when(orderRepository.existsByCustomerEmailAndStatusIn(eq(EMAIL), any())).thenReturn(true);
        assertTrue(consent.canSend(EMAIL, EmailFlow.CART_ABANDONMENT));
    }

    @Test
    void customersGetPostPurchaseEmailsWithoutSubscribing() {
        when(subscriberRepository.findById(EMAIL)).thenReturn(Optional.empty());

        assertTrue(consent.canSend(EMAIL, EmailFlow.POST_PURCHASE));
        assertTrue(consent.canSend(EMAIL, EmailFlow.REPLENISHMENT));
    }

    @Test
    void anUnsubscribeOrPauseStopsEverything() {
        subscribed(SubscriberStatus.PAUSED);
        for (EmailFlow flow : EmailFlow.values()) {
            assertFalse(consent.canSend(EMAIL, flow));
        }
        subscribed(SubscriberStatus.ACTIVE);
        when(suppressionRepository.existsById(EMAIL)).thenReturn(true);
        for (EmailFlow flow : EmailFlow.values()) {
            assertFalse(consent.canSend(EMAIL, flow));
        }
    }

    @Test
    void unsubscribingSuppressesTheAddressAndEndsItsFlows() {
        subscribed(SubscriberStatus.ACTIVE);
        FlowEnrollment active = new FlowEnrollment.Builder().setEnrollmentId("e1").setEmail(EMAIL)
                .setFlow(EmailFlow.WELCOME).setStatus(EnrollmentStatus.ACTIVE).setEnrolledAt(LocalDateTime.now()).build();
        when(enrollmentRepository.findByEmailAndStatus(EMAIL, EnrollmentStatus.ACTIVE)).thenReturn(List.of(active));

        consent.unsubscribe(EMAIL, "Unsubscribe page");

        verify(suppressionRepository).save(any(EmailSuppression.class));
        verify(subscriberRepository).save(argThat(s -> s.getStatus() == SubscriberStatus.UNSUBSCRIBED));
        verify(enrollmentRepository).save(argThat(e -> e.getStatus() == EnrollmentStatus.EXITED));
    }

    @Test
    void resubscribingLiftsTheSuppression() {
        when(subscriberRepository.findById(anyString())).thenReturn(Optional.empty());

        consent.resubscribe(EMAIL, "footer");

        verify(suppressionRepository).deleteById(EMAIL);
        verify(subscriberRepository).save(argThat(s -> s.getStatus() == SubscriberStatus.ACTIVE));
    }

    private void subscribed(SubscriberStatus status) {
        when(subscriberRepository.findById(EMAIL)).thenReturn(Optional.of(new NewsletterSubscriber.Builder()
                .setEmail(EMAIL).setSubscribedAt(Instant.now()).setStatus(status).build()));
    }
}
