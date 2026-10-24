package za.ac.cput.guiltfreecookie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.ac.cput.guiltfreecookie.domain.Customer;
import za.ac.cput.guiltfreecookie.domain.Name;
import za.ac.cput.guiltfreecookie.domain.PasswordResetToken;
import za.ac.cput.guiltfreecookie.dto.auth.CustomerAuthResponse;
import za.ac.cput.guiltfreecookie.dto.auth.LoginRequest;
import za.ac.cput.guiltfreecookie.dto.auth.ProfileRequest;
import za.ac.cput.guiltfreecookie.dto.auth.RegisterRequest;
import za.ac.cput.guiltfreecookie.dto.auth.ResetPasswordRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.UnauthorizedException;
import za.ac.cput.guiltfreecookie.repository.PasswordResetTokenRepository;
import za.ac.cput.guiltfreecookie.security.JwtService;
import za.ac.cput.guiltfreecookie.service.notification.EmailService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceTest {

    private static final String EMAIL = "thandi@example.co.za";
    private static final String PASSWORD = "cookies-4-life";

    @Mock
    private CustomerService customerService;
    @Mock
    private PasswordResetTokenRepository resetTokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private NewsletterService newsletterService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final JwtService jwtService = new JwtService("dGVzdC1vbmx5LXNlY3JldC10aGF0LWlzLTMyLWJ5dGVzISE=", 60);
    private CustomerAuthService service;
    private Customer thandi;

    @BeforeEach
    void setUp() {
        service = new CustomerAuthService(customerService, resetTokenRepository, passwordEncoder, jwtService,
                emailService, newsletterService, "https://shop.example", 60);
        thandi = new Customer.Builder()
                .setCustomerEmail(EMAIL)
                .setName(new Name.Builder().setFirstName("Thandi").setLastName("Mokoena").build())
                .setPasswordHash(passwordEncoder.encode(PASSWORD))
                .setTokenVersion(0)
                .build();
    }

    private RegisterRequest registration(String email, String password, String mobile, boolean newsletter) {
        return new RegisterRequest(email, password, "Thandi", null, "Mokoena", mobile, newsletter);
    }

    @Test
    void registerHashesThePasswordAndSignsIn() {
        when(customerService.create(any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        CustomerAuthResponse response = service.register(registration(" Thandi@Example.co.za ", PASSWORD, "082 123 4567", false));

        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(customerService).create(saved.capture());
        assertEquals(EMAIL, saved.getValue().getCustomerEmail());
        assertNotEquals(PASSWORD, saved.getValue().getPasswordHash());
        assertTrue(passwordEncoder.matches(PASSWORD, saved.getValue().getPasswordHash()));
        assertNotNull(response.token());
        verify(newsletterService, never()).subscribe(anyString());
    }

    @Test
    void registerCanOptIntoTheNewsletter() {
        when(customerService.create(any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        service.register(registration(EMAIL, PASSWORD, null, true));

        verify(newsletterService).subscribe(EMAIL);
    }

    @Test
    void registerRejectsAnEmailThatAlreadyExists() {
        when(customerService.read(EMAIL)).thenReturn(new Customer.Builder().setCustomerEmail(EMAIL).build());

        assertThrows(ConflictException.class, () -> service.register(registration(EMAIL, PASSWORD, null, false)));
        verify(customerService, never()).create(any());
    }

    @Test
    void registerValidatesInput() {
        assertThrows(BadRequestException.class, () -> service.register(registration("not-an-email", PASSWORD, null, false)));
        assertThrows(BadRequestException.class, () -> service.register(registration(EMAIL, "short", null, false)));
        assertThrows(BadRequestException.class, () -> service.register(registration(EMAIL, PASSWORD, "12345", false)));
        assertThrows(BadRequestException.class, () -> service.register(
                new RegisterRequest(EMAIL, PASSWORD, " ", null, "Mokoena", null, false)));
    }

    @Test
    void loginWithTheRightPasswordReturnsAToken() {
        when(customerService.read(EMAIL)).thenReturn(thandi);

        CustomerAuthResponse response = service.login(new LoginRequest("THANDI@example.co.za", PASSWORD));

        assertEquals(EMAIL, response.customer().getCustomerEmail());
        assertEquals(EMAIL, jwtService.parse(response.token()).orElseThrow().getSubject());
    }

    @Test
    void loginFailsTheSameWayForEveryKindOfBadCredential() {
        Customer noAccount = new Customer.Builder().setCustomerEmail("walkin@example.co.za").build();
        when(customerService.read(EMAIL)).thenReturn(thandi);
        when(customerService.read("walkin@example.co.za")).thenReturn(noAccount);
        when(customerService.read("nobody@example.co.za")).thenReturn(null);

        UnauthorizedException wrongPassword = assertThrows(UnauthorizedException.class,
                () -> service.login(new LoginRequest(EMAIL, "wrong-password")));
        UnauthorizedException unknown = assertThrows(UnauthorizedException.class,
                () -> service.login(new LoginRequest("nobody@example.co.za", PASSWORD)));
        UnauthorizedException passwordless = assertThrows(UnauthorizedException.class,
                () -> service.login(new LoginRequest("walkin@example.co.za", PASSWORD)));

        assertEquals(wrongPassword.getMessage(), unknown.getMessage());
        assertEquals(wrongPassword.getMessage(), passwordless.getMessage());
    }

    @Test
    void forgotPasswordForAnUnknownEmailDoesNothingVisible() {
        service.forgotPassword("nobody@example.co.za");

        verify(resetTokenRepository, never()).save(any());
        org.mockito.Mockito.verifyNoInteractions(emailService);
    }

    @Test
    void forgotPasswordStoresOnlyTheHashOfTheEmailedToken() {
        when(customerService.read(EMAIL)).thenReturn(thandi);

        service.forgotPassword(EMAIL);

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<java.util.Map<String,Object>> body = ArgumentCaptor.forClass(java.util.Map.class);
        verify(resetTokenRepository).deleteByCustomerEmail(EMAIL);
        verify(resetTokenRepository).save(saved.capture());
        verify(emailService).sendTemplate(eq(EMAIL), anyString(), eq("password-reset"), body.capture(), eq(false));

        String emailed = body.getValue().get("actionUrl").toString().replaceAll("(?s).*token=(\\S+).*", "$1");
        assertTrue(body.getValue().get("actionUrl").toString().contains("https://shop.example/reset-password?token="));
        assertNotEquals(emailed, saved.getValue().getTokenHash());
        assertEquals(CustomerAuthService.sha256(emailed), saved.getValue().getTokenHash());
    }

    @Test
    void resetPasswordSetsTheNewPasswordAndEndsOldSessions() {
        PasswordResetToken token = new PasswordResetToken.Builder()
                .setTokenHash(CustomerAuthService.sha256("raw-token"))
                .setCustomerEmail(EMAIL)
                .setExpiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        when(resetTokenRepository.findById(token.getTokenHash())).thenReturn(Optional.of(token));
        when(customerService.read(EMAIL)).thenReturn(thandi);
        when(customerService.save(any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        CustomerAuthResponse response = service.resetPassword(new ResetPasswordRequest("raw-token", "brand-new-pass"));

        assertTrue(passwordEncoder.matches("brand-new-pass", response.customer().getPasswordHash()));
        assertEquals(1, response.customer().getTokenVersion());
        verify(resetTokenRepository).deleteByCustomerEmail(EMAIL);
    }

    @Test
    void resetPasswordRejectsExpiredAndUnknownTokens() {
        PasswordResetToken expired = new PasswordResetToken.Builder()
                .setTokenHash(CustomerAuthService.sha256("old-token"))
                .setCustomerEmail(EMAIL)
                .setExpiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(resetTokenRepository.findById(expired.getTokenHash())).thenReturn(Optional.of(expired));

        assertThrows(BadRequestException.class,
                () -> service.resetPassword(new ResetPasswordRequest("old-token", "brand-new-pass")));
        assertThrows(BadRequestException.class,
                () -> service.resetPassword(new ResetPasswordRequest("made-up", "brand-new-pass")));
        verify(customerService, never()).save(any());
    }

    @Test
    void changePasswordNeedsTheCurrentPassword() {
        when(customerService.read(EMAIL)).thenReturn(thandi);

        assertThrows(UnauthorizedException.class, () -> service.changePassword(EMAIL, "wrong-password", "brand-new-pass"));
        verify(customerService, never()).save(any());
    }

    @Test
    void changePasswordBumpsTheTokenVersion() {
        when(customerService.read(EMAIL)).thenReturn(thandi);
        when(customerService.save(any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        CustomerAuthResponse response = service.changePassword(EMAIL, PASSWORD, "brand-new-pass");

        assertEquals(1, response.customer().getTokenVersion());
        assertTrue(passwordEncoder.matches("brand-new-pass", response.customer().getPasswordHash()));
    }

    @Test
    void updateProfileValidatesTheMobileNumber() {
        when(customerService.read(EMAIL)).thenReturn(thandi);

        assertThrows(BadRequestException.class,
                () -> service.updateProfile(EMAIL, new ProfileRequest("Thandi", null, "Mokoena", "999")));
        verify(customerService, never()).update(any());
    }
}
