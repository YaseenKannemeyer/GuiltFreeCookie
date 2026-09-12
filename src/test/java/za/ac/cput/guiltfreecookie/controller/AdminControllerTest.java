package za.ac.cput.guiltfreecookie.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import za.ac.cput.guiltfreecookie.domain.Admin;
import za.ac.cput.guiltfreecookie.security.JwtService;
import za.ac.cput.guiltfreecookie.service.AdminService;
import za.ac.cput.guiltfreecookie.service.PasswordResetResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminController adminController;

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

    private ChangePasswordRequest changePasswordRequest(String currentPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        ReflectionTestUtils.setField(request, "currentPassword", currentPassword);
        ReflectionTestUtils.setField(request, "newPassword", newPassword);
        return request;
    }

    @Test
    void testCreate() {
        when(adminService.create(existingAdmin)).thenReturn(existingAdmin);

        ResponseEntity<Admin> result = adminController.create(existingAdmin);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("AD001", result.getBody().getAdminId());
    }

    @Test
    void testCreateWithInvalidEmailReturnsBadRequestAndNeverPersists() {
        Admin invalid = new Admin.Builder().copy(existingAdmin).setEmail("not-an-email").build();

        ResponseEntity<Admin> result = adminController.create(invalid);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verify(adminService, never()).create(any(Admin.class));
    }

    @Test
    void testCreateWithMissingFirstNameReturnsBadRequestAndNeverPersists() {
        Admin invalid = new Admin.Builder().copy(existingAdmin).setFirstName("").build();

        ResponseEntity<Admin> result = adminController.create(invalid);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verify(adminService, never()).create(any(Admin.class));
    }

    @Test
    void testGetAll() {
        when(adminService.getAll()).thenReturn(List.of(existingAdmin));

        ResponseEntity<List<Admin>> result = adminController.getAll();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void testReadFound() {
        when(adminService.read("AD001")).thenReturn(existingAdmin);

        ResponseEntity<Admin> result = adminController.read("AD001");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("jane.doe@guiltfreecookie.com", result.getBody().getEmail());
    }

    @Test
    void testReadNotFound() {
        when(adminService.read("missing")).thenReturn(null);

        ResponseEntity<Admin> result = adminController.read("missing");

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void testUpdate() {
        Admin updated = new Admin.Builder().copy(existingAdmin).setFirstName("Janet").build();
        when(adminService.update(existingAdmin)).thenReturn(updated);

        ResponseEntity<Admin> result = adminController.update(existingAdmin);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Janet", result.getBody().getFirstName());
    }

    @Test
    void testUpdateWithInvalidEmailReturnsBadRequestAndNeverPersists() {
        Admin invalid = new Admin.Builder().copy(existingAdmin).setEmail("not-an-email").build();

        ResponseEntity<Admin> result = adminController.update(invalid);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verify(adminService, never()).update(any(Admin.class));
    }

    @Test
    void testUpdateWhenServiceReturnsNullReturnsNotFound() {
        when(adminService.update(existingAdmin)).thenReturn(null);

        ResponseEntity<Admin> result = adminController.update(existingAdmin);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void testDelete() {
        ResponseEntity<Void> result = adminController.delete("AD001");

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(adminService).delete("AD001");
    }

    @Test
    void testLoginSuccessReturnsAdminAndToken() {
        Admin credentials = new Admin.Builder()
                .setEmail("jane.doe@guiltfreecookie.com")
                .setPassword("SecurePass123")
                .build();
        when(adminService.login("jane.doe@guiltfreecookie.com", "SecurePass123")).thenReturn(existingAdmin);
        when(jwtService.generateToken("AD001", "jane.doe@guiltfreecookie.com")).thenReturn("fake.jwt.token");

        ResponseEntity<?> result = adminController.login(credentials);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        LoginResponse body = (LoginResponse) result.getBody();
        assertEquals(existingAdmin, body.getAdmin());
        assertEquals("fake.jwt.token", body.getToken());
    }

    @Test
    void testLoginFailureReturnsUnauthorized() {
        Admin credentials = new Admin.Builder()
                .setEmail("jane.doe@guiltfreecookie.com")
                .setPassword("WrongPassword")
                .build();
        when(adminService.login("jane.doe@guiltfreecookie.com", "WrongPassword")).thenReturn(null);

        ResponseEntity<?> result = adminController.login(credentials);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals("Invalid email or password", result.getBody());
    }

    @Test
    void testSetActiveFound() {
        Admin deactivated = new Admin.Builder().copy(existingAdmin).setActive(false).build();
        when(adminService.setActive("AD001", false)).thenReturn(deactivated);

        ResponseEntity<Admin> result = adminController.setActive("AD001", false);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertFalse(result.getBody().isActive());
    }

    @Test
    void testSetActiveNotFound() {
        when(adminService.setActive("missing", true)).thenReturn(null);

        ResponseEntity<Admin> result = adminController.setActive("missing", true);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void testResetPasswordFoundReturnsAdminAndTemporaryPassword() {
        Admin reset = new Admin.Builder().copy(existingAdmin).setPassword("hashed-temp").build();
        PasswordResetResult resetResult = new PasswordResetResult(reset, "TempPass1234");
        when(adminService.resetPassword("AD001")).thenReturn(resetResult);

        ResponseEntity<PasswordResetResult> result = adminController.resetPassword("AD001");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("TempPass1234", result.getBody().getTemporaryPassword());
        assertEquals("hashed-temp", result.getBody().getAdmin().getPassword());
    }

    @Test
    void testResetPasswordNotFound() {
        when(adminService.resetPassword("missing")).thenReturn(null);

        ResponseEntity<PasswordResetResult> result = adminController.resetPassword("missing");

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void testChangePasswordWhenAdminNotFoundReturnsNotFound() {
        when(adminService.read("missing")).thenReturn(null);

        ResponseEntity<?> result = adminController.changePassword(
                "missing", changePasswordRequest("old", "new"));

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        verify(adminService, never()).changePassword(any(), any());
    }

    @Test
    void testChangePasswordWithWrongCurrentPasswordReturnsUnauthorized() {
        when(adminService.read("AD001")).thenReturn(existingAdmin);
        when(passwordEncoder.matches("WrongCurrent", existingAdmin.getPassword())).thenReturn(false);

        ResponseEntity<?> result = adminController.changePassword(
                "AD001", changePasswordRequest("WrongCurrent", "NewPassword456"));

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals("Current password is incorrect", result.getBody());
        verify(adminService, never()).changePassword(any(), any());
    }

    @Test
    void testChangePasswordWithEmptyNewPasswordReturnsBadRequest() {
        when(adminService.read("AD001")).thenReturn(existingAdmin);
        when(passwordEncoder.matches("SecurePass123", existingAdmin.getPassword())).thenReturn(true);

        ResponseEntity<?> result = adminController.changePassword(
                "AD001", changePasswordRequest("SecurePass123", ""));

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verify(adminService, never()).changePassword(any(), any());
    }

    @Test
    void testChangePasswordWithCorrectCurrentPasswordSucceeds() {
        Admin changed = new Admin.Builder().copy(existingAdmin).setPassword("hashed-new-password").build();
        when(adminService.read("AD001")).thenReturn(existingAdmin);
        when(passwordEncoder.matches("SecurePass123", existingAdmin.getPassword())).thenReturn(true);
        when(adminService.changePassword("AD001", "NewPassword456")).thenReturn(changed);

        ResponseEntity<?> result = adminController.changePassword(
                "AD001", changePasswordRequest("SecurePass123", "NewPassword456"));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(changed, result.getBody());
        verify(adminService).changePassword("AD001", "NewPassword456");
    }
}
