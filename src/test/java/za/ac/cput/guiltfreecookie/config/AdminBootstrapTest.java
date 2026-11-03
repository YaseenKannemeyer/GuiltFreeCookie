package za.ac.cput.guiltfreecookie.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.guiltfreecookie.repository.AdminRepository;
import za.ac.cput.guiltfreecookie.service.AdminService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock
    private AdminRepository adminRepository;
    @Mock
    private AdminService adminService;

    @Test
    void anEmptyDatabaseGetsItsFirstAdmin() {
        when(adminRepository.count()).thenReturn(0L);

        new AdminBootstrap(adminRepository, adminService, " Owner@GuiltFreeCookie.co.za ", "a-long-first-password").createFirstAdmin();

        verify(adminService).create(argThat(admin -> admin.getEmail().equals("owner@guiltfreecookie.co.za") && admin.isActive()));
    }

    @Test
    void doesNothingOnceAnAdminExists() {
        when(adminRepository.count()).thenReturn(1L);

        new AdminBootstrap(adminRepository, adminService, "owner@guiltfreecookie.co.za", "a-long-first-password").createFirstAdmin();

        verify(adminService, never()).create(any());
    }

    @Test
    void refusesAShortPasswordAndIgnoresAnUnsetEmail() {
        when(adminRepository.count()).thenReturn(0L);

        new AdminBootstrap(adminRepository, adminService, "owner@guiltfreecookie.co.za", "short").createFirstAdmin();
        new AdminBootstrap(adminRepository, adminService, "", "a-long-first-password").createFirstAdmin();

        verify(adminService, never()).create(any());
    }
}
