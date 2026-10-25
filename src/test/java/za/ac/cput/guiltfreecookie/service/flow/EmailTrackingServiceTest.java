package za.ac.cput.guiltfreecookie.service.flow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.guiltfreecookie.domain.EmailFlow;
import za.ac.cput.guiltfreecookie.domain.EmailSend;
import za.ac.cput.guiltfreecookie.repository.EmailSendRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailTrackingServiceTest {

    private static final String SEND_ID = "abcdefghijklmnopqrstuvwxyz012345";

    @Mock
    private EmailSendRepository sendRepository;
    @Mock
    private MarketingConsent consent;

    private final MarketingLinks links = new MarketingLinks("secret", "https://api.example", "https://shop.example");

    @Test
    void aClickIsRecordedOnceAndCountsAsEngagement() {
        EmailTrackingService service = new EmailTrackingService(sendRepository, consent, links);
        when(sendRepository.findById(SEND_ID)).thenReturn(Optional.of(send()));

        String destination = service.clicked(SEND_ID, "https://shop.example/cart");

        assertEquals("https://shop.example/cart", destination);
        verify(sendRepository).save(argThat(saved -> saved.getClickedAt() != null && saved.getOpenedAt() != null));
        verify(consent).markEngaged("thandi@example.co.za");
    }

    @Test
    void aLinkToAnotherSiteGoesHomeInstead() {
        EmailTrackingService service = new EmailTrackingService(sendRepository, consent, links);

        assertEquals("https://shop.example/", service.clicked("not-a-send", "https://evil.example.com/"));
        verify(sendRepository, never()).findById(anyString());
    }

    @Test
    void anOpenIsOnlyRecordedTheFirstTime() {
        EmailTrackingService service = new EmailTrackingService(sendRepository, consent, links);
        when(sendRepository.findById(SEND_ID)).thenReturn(Optional.of(new EmailSend.Builder().copy(send())
                .setOpenedAt(LocalDateTime.now()).build()));

        service.opened(SEND_ID);

        verify(sendRepository, never()).save(any());
    }

    private static EmailSend send() {
        return new EmailSend.Builder().setSendId(SEND_ID).setEmail("thandi@example.co.za").setFlow(EmailFlow.WELCOME)
                .setStep(0).setSentAt(LocalDateTime.now()).build();
    }
}
