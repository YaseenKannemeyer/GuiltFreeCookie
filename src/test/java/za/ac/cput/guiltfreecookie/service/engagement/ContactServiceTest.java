package za.ac.cput.guiltfreecookie.service.engagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.guiltfreecookie.domain.ContactMessage;
import za.ac.cput.guiltfreecookie.domain.ContactStatus;
import za.ac.cput.guiltfreecookie.dto.engagement.ContactRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.ContactMessageRepository;
import za.ac.cput.guiltfreecookie.service.notification.EmailService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactMessageRepository messageRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private SiteInfoService siteInfoService;
    @InjectMocks
    private ContactService service;

    @Test
    void aFilledHoneypotIsDroppedWithoutSavingOrSending() {
        service.submit(new ContactRequest(null, null, null, null, null, "spam"));

        verifyNoInteractions(messageRepository, emailService);
    }

    @Test
    void aValidMessageIsSavedThenTheShopAndCustomerAreEmailed() {
        when(siteInfoService.setting("support-email")).thenReturn("shop@example.com");
        when(messageRepository.save(any(ContactMessage.class)))
                .thenAnswer(call -> new ContactMessage.Builder().copy(call.getArgument(0)).setId("m1").build());

        service.submit(new ContactRequest(" Sam ", " SAM@example.com ", null, "Question", "Hello", ""));

        verify(messageRepository).save(argThat(message -> message.getEmail().equals("sam@example.com")
                && message.getName().equals("Sam")
                && message.getStatus() == ContactStatus.NEW));
        verify(emailService).sendTemplate(eq("shop@example.com"), anyString(), eq("contact-to-shop"), anyMap(), eq(false));
        verify(emailService).sendTemplate(eq("sam@example.com"), anyString(), eq("contact-auto-reply"), anyMap(), eq(false));
    }

    @Test
    void invalidInputIsNotSaved() {
        assertThrows(BadRequestException.class,
                () -> service.submit(new ContactRequest("Sam", "bad", "", "Question", "Hello", "")));
        assertThrows(BadRequestException.class,
                () -> service.submit(new ContactRequest("Sam", "sam@example.com", "12345", "Question", "Hello", "")));

        verifyNoInteractions(messageRepository, emailService);
    }

    @Test
    void resolvingRecordsWhoAndWhenAndOnlyHappensOnce() {
        ContactMessage message = new ContactMessage.Builder().setId("m1").setStatus(ContactStatus.NEW).build();
        when(messageRepository.findById("m1")).thenReturn(Optional.of(message));
        when(messageRepository.save(any(ContactMessage.class))).thenAnswer(call -> call.getArgument(0));

        ContactMessage resolved = service.resolve("m1", "a1");

        assertEquals("a1", resolved.getResolvedBy());
        assertNotNull(resolved.getResolvedAt());
        when(messageRepository.findById("m1")).thenReturn(Optional.of(resolved));
        assertSame(resolved, service.resolve("m1", "a2"));
        verify(messageRepository, times(1)).save(any(ContactMessage.class));
        assertThrows(NotFoundException.class, () -> service.resolve("missing", "a1"));
    }
}
