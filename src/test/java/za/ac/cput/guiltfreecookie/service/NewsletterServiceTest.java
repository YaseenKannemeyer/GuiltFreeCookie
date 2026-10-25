package za.ac.cput.guiltfreecookie.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.guiltfreecookie.domain.NewsletterSubscriber;
import za.ac.cput.guiltfreecookie.domain.SubscriberStatus;
import za.ac.cput.guiltfreecookie.repository.NewsletterSubscriberRepository;
import za.ac.cput.guiltfreecookie.service.flow.FlowEnrollmentService;
import za.ac.cput.guiltfreecookie.service.flow.MarketingConsent;
import za.ac.cput.guiltfreecookie.service.flow.ResubscribeConfirmation;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceTest {

    private static final String EMAIL = "thandi@example.co.za";

    @Mock
    private NewsletterSubscriberRepository repository;

    @Mock
    private MarketingConsent consent;

    @Mock
    private FlowEnrollmentService flows;

    @Mock
    private ResubscribeConfirmation confirmation;

    @InjectMocks
    private NewsletterService newsletterService;

    @Test
    void savesANewSubscriberWithTheirOptInTimeAndStartsTheWelcome() {
        when(repository.findById(EMAIL)).thenReturn(Optional.empty());

        newsletterService.subscribe(EMAIL, "footer");

        verify(repository).save(argThat(s -> s.getEmail().equals(EMAIL) && s.getSubscribedAt() != null
                && s.getStatus() == SubscriberStatus.ACTIVE && "footer".equals(s.getSource())));
        verify(flows).welcome(EMAIL);
    }

    @Test
    void keepsTheOriginalOptInWhenSubscribingAgain() {
        when(repository.findById(EMAIL)).thenReturn(Optional.of(new NewsletterSubscriber(EMAIL, Instant.now())));

        newsletterService.subscribe(EMAIL);

        verify(repository, never()).save(any(NewsletterSubscriber.class));
        verify(consent, never()).resubscribe(anyString(), any());
    }

    @Test
    void someoneWhoUnsubscribedMustConfirmFromTheirInbox() {
        when(repository.findById(EMAIL)).thenReturn(Optional.of(new NewsletterSubscriber.Builder()
                .setEmail(EMAIL).setSubscribedAt(Instant.now()).setStatus(SubscriberStatus.UNSUBSCRIBED).build()));

        newsletterService.subscribe(EMAIL, "footer");

        verify(confirmation).send(EMAIL);
        verify(consent, never()).resubscribe(anyString(), any());
        verify(flows, never()).welcome(anyString());
    }
}
