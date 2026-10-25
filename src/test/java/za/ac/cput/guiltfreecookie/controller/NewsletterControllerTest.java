package za.ac.cput.guiltfreecookie.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import za.ac.cput.guiltfreecookie.service.NewsletterService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NewsletterControllerTest {

    @Mock
    private NewsletterService newsletterService;

    @InjectMocks
    private NewsletterController newsletterController;

    @Test
    void subscribesANormalisedEmail() {
        ResponseEntity<Void> result = newsletterController.subscribe(
                new NewsletterController.SubscribeRequest("  Thandi@Example.CO.ZA "));

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(newsletterService).subscribe("thandi@example.co.za", "footer");
    }

    @Test
    void rejectsAnInvalidEmail() {
        ResponseEntity<Void> result = newsletterController.subscribe(
                new NewsletterController.SubscribeRequest("not an email"));

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verify(newsletterService, never()).subscribe(anyString(), any());
    }

    @Test
    void rejectsAMissingBody() {
        ResponseEntity<Void> result = newsletterController.subscribe(null);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }
}
