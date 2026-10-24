package za.ac.cput.guiltfreecookie.service.engagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.*;
import za.ac.cput.guiltfreecookie.domain.*;
import za.ac.cput.guiltfreecookie.dto.engagement.*;
import za.ac.cput.guiltfreecookie.exception.*;
import za.ac.cput.guiltfreecookie.repository.*;
import za.ac.cput.guiltfreecookie.service.notification.EmailService;

class ContactServiceTest {

    ContactMessageRepository repo = mock(ContactMessageRepository.class);
    EmailService email = mock(EmailService.class);
    SiteInfoService site = mock(SiteInfoService.class);
    ContactService service = new ContactService(repo, email, site);

    @Test
    void honeypotDropsBeforeValidationAndSendsNothing() {
        service.submit(
            new ContactRequest(null, null, null, null, null, "spam")
        );
        verifyNoInteractions(repo, email);
    }

    @Test
    void validMessageIsSavedThenBothEmailsQueued() {
        when(site.setting("support-email")).thenReturn("shop@example.com");
        when(repo.save(any())).thenAnswer(i ->
            new ContactMessage.Builder()
                .copy(i.getArgument(0))
                .setId("m1")
                .build()
        );
        service.submit(
            new ContactRequest(
                " Sam ",
                " SAM@example.com ",
                null,
                "Question",
                "Hello",
                ""
            )
        );
        verify(repo).save(
            argThat(
                m ->
                    m.getEmail().equals("sam@example.com") &&
                    m.getName().equals("Sam") &&
                    m.getStatus() == ContactStatus.NEW
            )
        );
        verify(email).sendTemplate(
            eq("shop@example.com"),
            anyString(),
            eq("contact-to-shop"),
            anyMap(),
            eq(false)
        );
        verify(email).sendTemplate(
            eq("sam@example.com"),
            anyString(),
            eq("contact-auto-reply"),
            anyMap(),
            eq(false)
        );
    }

    @Test
    void invalidInputDoesNotSave() {
        assertThrows(BadRequestException.class, () ->
            service.submit(
                new ContactRequest("Sam", "bad", "", "Question", "Hello", "")
            )
        );
        verifyNoInteractions(repo, email);
    }

    @Test
    void resolutionIsAuditedAndIdempotent() {
        var message = new ContactMessage.Builder()
            .setId("m1")
            .setStatus(ContactStatus.NEW)
            .build();
        when(repo.findById("m1")).thenReturn(Optional.of(message));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.resolve("m1", "a1");
        assertEquals("a1", result.getResolvedBy());
        assertNotNull(result.getResolvedAt());
        when(repo.findById("m1")).thenReturn(Optional.of(result));
        assertSame(result, service.resolve("m1", "a2"));
        verify(repo, times(1)).save(any());
        assertThrows(NotFoundException.class, () ->
            service.resolve("missing", "a1")
        );
    }
}
