package za.ac.cput.guiltfreecookie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.ac.cput.guiltfreecookie.domain.Admin;
import za.ac.cput.guiltfreecookie.repository.AdminRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private static final Pattern TEMP_PASSWORD_PATTERN =
            Pattern.compile("^[ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789]{10}$");

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    private Admin existingAdmin;

    @BeforeEach
    void setUp() {
        existingAdmin = new Admin.Builder()
                .setAdminId("AD001")
                .setFirstName("Jane")
                .setLastName("Doe")
                .setEmail("jane.doe@guiltfreecookie.com")
                .setPassword("$2a$10$hashedSecurePass123")
                .setActive(true)
                .build();
    }

    @Test
    void testCreateHashesThePasswordBeforeSaving() {
        Admin incoming = new Admin.Builder().copy(existingAdmin).setPassword("SecurePass123").build();
        when(passwordEncoder.encode("SecurePass123")).thenReturn("hashed-secure-pass-123");
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Admin result = adminService.create(incoming);

        assertEquals("AD001", result.getAdminId());
        assertEquals("hashed-secure-pass-123", result.getPassword());
        verify(adminRepository).save(argThatPasswordEquals("hashed-secure-pass-123"));
    }

    @Test
    void testReadFound() {
        when(adminRepository.findById("AD001")).thenReturn(Optional.of(existingAdmin));

        Admin result = adminService.read("AD001");

        assertEquals("jane.doe@guiltfreecookie.com", result.getEmail());
    }

    @Test
    void testReadNotFound() {
        when(adminRepository.findById("missing")).thenReturn(Optional.empty());

        Admin result = adminService.read("missing");

        assertNull(result);
    }

    @Test
    void testUpdateMergesProfileFieldsAndPreservesCredentials() {
        Admin incoming = new Admin.Builder()
                .setAdminId("AD001")
                .setFirstName("Janet")
                .setLastName("Smith")
                .setEmail("janet.smith@guiltfreecookie.com")
                .build();

        when(adminRepository.findById("AD001")).thenReturn(Optional.of(existingAdmin));
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Admin result = adminService.update(incoming);

        assertEquals("Janet", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        assertEquals("janet.smith@guiltfreecookie.com", result.getEmail());
        assertEquals(existingAdmin.getPassword(), result.getPassword(), "update() must not touch the password");
        assertTrue(result.isActive(), "update() must not touch the active flag");
    }

    @Test
    void testUpdateWhenNotFoundReturnsNull() {
        when(adminRepository.findById("missing")).thenReturn(Optional.empty());

        Admin result = adminService.update(new Admin.Builder().setAdminId("missing").build());

        assertNull(result);
        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void testDeleteWhenExists() {
        when(adminRepository.existsById("AD001")).thenReturn(true);

        boolean result = adminService.delete("AD001");

        assertTrue(result);
        verify(adminRepository).deleteById("AD001");
    }

    @Test
    void testDeleteWhenNotExists() {
        when(adminRepository.existsById("missing")).thenReturn(false);

        boolean result = adminService.delete("missing");

        assertFalse(result);
        verify(adminRepository, never()).deleteById(anyString());
    }

    @Test
    void testGetAllReturnsEveryAdmin() {
        when(adminRepository.findAll()).thenReturn(List.of(existingAdmin));

        List<Admin> result = adminService.getAll();

        assertEquals(1, result.size());
    }

    @Test
    void testLoginSucceedsForActiveAdminWithCorrectPassword() {
        when(adminRepository.findByEmail("jane.doe@guiltfreecookie.com")).thenReturn(Optional.of(existingAdmin));
        when(passwordEncoder.matches("SecurePass123", existingAdmin.getPassword())).thenReturn(true);

        Admin result = adminService.login("jane.doe@guiltfreecookie.com", "SecurePass123");

        assertEquals("AD001", result.getAdminId());
    }

    @Test
    void testLoginFailsForWrongPassword() {
        when(adminRepository.findByEmail("jane.doe@guiltfreecookie.com")).thenReturn(Optional.of(existingAdmin));
        when(passwordEncoder.matches("WrongPassword", existingAdmin.getPassword())).thenReturn(false);

        Admin result = adminService.login("jane.doe@guiltfreecookie.com", "WrongPassword");

        assertNull(result);
    }

    @Test
    void testLoginFailsWhenEmailNotFound() {
        when(adminRepository.findByEmail("unknown@guiltfreecookie.com")).thenReturn(Optional.empty());

        Admin result = adminService.login("unknown@guiltfreecookie.com", "whatever");

        assertNull(result);
    }

    @Test
    void testLoginFailsForInactiveAdminEvenWithCorrectPassword() {
        Admin inactiveAdmin = new Admin.Builder().copy(existingAdmin).setActive(false).build();
        when(adminRepository.findByEmail("jane.doe@guiltfreecookie.com")).thenReturn(Optional.of(inactiveAdmin));

        Admin result = adminService.login("jane.doe@guiltfreecookie.com", "SecurePass123");

        assertNull(result);
    }

    @Test
    void testLoginFailsWhenTemporaryPasswordHasExpired() {
        Admin expired = new Admin.Builder()
                .copy(existingAdmin)
                .setPasswordExpiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(adminRepository.findByEmail("jane.doe@guiltfreecookie.com")).thenReturn(Optional.of(expired));

        Admin result = adminService.login("jane.doe@guiltfreecookie.com", "SecurePass123");

        assertNull(result);
    }

    @Test
    void testLoginSucceedsWhenTemporaryPasswordHasNotExpiredYet() {
        Admin stillValid = new Admin.Builder()
                .copy(existingAdmin)
                .setPasswordExpiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(adminRepository.findByEmail("jane.doe@guiltfreecookie.com")).thenReturn(Optional.of(stillValid));
        when(passwordEncoder.matches("SecurePass123", existingAdmin.getPassword())).thenReturn(true);

        Admin result = adminService.login("jane.doe@guiltfreecookie.com", "SecurePass123");

        assertEquals("AD001", result.getAdminId());
    }

    @Test
    void testSetActiveTrue() {
        Admin inactive = new Admin.Builder().copy(existingAdmin).setActive(false).build();
        when(adminRepository.findById("AD001")).thenReturn(Optional.of(inactive));
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Admin result = adminService.setActive("AD001", true);

        assertTrue(result.isActive());
    }

    @Test
    void testSetActiveFalse() {
        when(adminRepository.findById("AD001")).thenReturn(Optional.of(existingAdmin));
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Admin result = adminService.setActive("AD001", false);

        assertFalse(result.isActive());
    }

    @Test
    void testSetActiveWhenNotFoundReturnsNull() {
        when(adminRepository.findById("missing")).thenReturn(Optional.empty());

        Admin result = adminService.setActive("missing", true);

        assertNull(result);
        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void testResetPasswordGeneratesFreshTemporaryPasswordWithTwentyFourHourExpiry() {
        when(adminRepository.findById("AD001")).thenReturn(Optional.of(existingAdmin));
        ArgumentCaptor<String> plainPasswordCaptor = ArgumentCaptor.forClass(String.class);
        when(passwordEncoder.encode(plainPasswordCaptor.capture())).thenReturn("hashed-temp-password");
        ArgumentCaptor<Admin> savedAdminCaptor = ArgumentCaptor.forClass(Admin.class);
        when(adminRepository.save(savedAdminCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        PasswordResetResult result = adminService.resetPassword("AD001");

        String plainTempPassword = result.getTemporaryPassword();
        assertNotEquals("SecurePass123", plainTempPassword, "resetPassword() must replace the old password");
        assertTrue(TEMP_PASSWORD_PATTERN.matcher(plainTempPassword).matches(),
                "temp password should be 10 chars from the unambiguous charset, was: " + plainTempPassword);
        assertEquals(plainTempPassword, plainPasswordCaptor.getValue(),
                "the exact plain temp password returned to the caller must be what got hashed");
        assertEquals("hashed-temp-password", result.getAdmin().getPassword(),
                "the persisted admin must carry the hash, never the plain temp password");

        assertNotEquals(null, result.getAdmin().getPasswordExpiresAt());
        long minutesUntilExpiry = Duration.between(LocalDateTime.now(), result.getAdmin().getPasswordExpiresAt()).toMinutes();
        assertTrue(minutesUntilExpiry >= 23 * 60 && minutesUntilExpiry <= 24 * 60,
                "expiry should be ~24 hours out, was " + minutesUntilExpiry + " minutes");

        assertEquals("AD001", savedAdminCaptor.getValue().getAdminId());
    }

    @Test
    void testResetPasswordWhenNotFoundReturnsNull() {
        when(adminRepository.findById("missing")).thenReturn(Optional.empty());

        PasswordResetResult result = adminService.resetPassword("missing");

        assertNull(result);
        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void testChangePasswordHashesTheNewPasswordAndClearsExpiry() {
        Admin withExpiry = new Admin.Builder()
                .copy(existingAdmin)
                .setPasswordExpiresAt(LocalDateTime.now().plusHours(2))
                .build();
        when(adminRepository.findById("AD001")).thenReturn(Optional.of(withExpiry));
        when(passwordEncoder.encode("BrandNewPassword456")).thenReturn("hashed-brand-new-password");
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Admin result = adminService.changePassword("AD001", "BrandNewPassword456");

        assertEquals("hashed-brand-new-password", result.getPassword());
        assertNull(result.getPasswordExpiresAt(), "a self-chosen password should not expire");
    }

    @Test
    void testChangePasswordWhenNotFoundReturnsNull() {
        when(adminRepository.findById("missing")).thenReturn(Optional.empty());

        Admin result = adminService.changePassword("missing", "whatever");

        assertNull(result);
        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void testMigratePlaintextPasswordsHashesOnlyAdminsNotAlreadyHashed() {
        Admin plainTextAdmin = new Admin.Builder().copy(existingAdmin).setPassword("StillPlainText").build();
        Admin alreadyHashedAdmin = new Admin.Builder()
                .copy(existingAdmin)
                .setAdminId("AD002")
                .setPassword("$2b$10$alreadyHashedValueLooksLikeThis1234567890")
                .build();
        when(adminRepository.findAll()).thenReturn(List.of(plainTextAdmin, alreadyHashedAdmin));
        when(passwordEncoder.encode("StillPlainText")).thenReturn("hashed-still-plain-text");
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminService.migratePlaintextPasswords();

        verify(adminRepository).save(argThat(a -> "AD001".equals(a.getAdminId())
                && "hashed-still-plain-text".equals(a.getPassword())));
        verify(adminRepository, never()).save(argThat(a -> "AD002".equals(a.getAdminId())));
    }

    private Admin argThatPasswordEquals(String expectedPassword) {
        return argThat(a -> expectedPassword.equals(a.getPassword()));
    }
}
