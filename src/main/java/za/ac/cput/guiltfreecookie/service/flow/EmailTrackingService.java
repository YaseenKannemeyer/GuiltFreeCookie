package za.ac.cput.guiltfreecookie.service.flow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.EmailSend;
import za.ac.cput.guiltfreecookie.repository.EmailSendRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class EmailTrackingService {

    private static final Pattern SEND_ID = Pattern.compile("[A-Za-z0-9_-]{32}");

    private final EmailSendRepository sendRepository;
    private final MarketingConsent consent;
    private final MarketingLinks links;

    public EmailTrackingService(EmailSendRepository sendRepository, MarketingConsent consent, MarketingLinks links) {
        this.sendRepository = sendRepository;
        this.consent = consent;
        this.links = links;
    }

    @Transactional
    public void opened(String sendId) {
        find(sendId).filter(send -> send.getOpenedAt() == null).ifPresent(send -> {
            sendRepository.save(new EmailSend.Builder().copy(send).setOpenedAt(LocalDateTime.now()).build());
            consent.markEngaged(send.getEmail());
        });
    }

    // Returns where to send the browser: the requested storefront page, or the home page for anything else.
    @Transactional
    public String clicked(String sendId, String target) {
        String destination = links.isStorefront(target) ? target : links.frontend("/");
        find(sendId).ifPresent(send -> {
            if (send.getClickedAt() == null) {
                LocalDateTime now = LocalDateTime.now();
                sendRepository.save(new EmailSend.Builder().copy(send).setClickedAt(now)
                        .setOpenedAt(send.getOpenedAt() == null ? now : send.getOpenedAt()).build());
            }
            consent.markEngaged(send.getEmail());
        });
        return destination;
    }

    private Optional<EmailSend> find(String sendId) {
        return sendId != null && SEND_ID.matcher(sendId).matches() ? sendRepository.findById(sendId) : Optional.empty();
    }
}
