package za.ac.cput.guiltfreecookie.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import za.ac.cput.guiltfreecookie.domain.Admin;
import za.ac.cput.guiltfreecookie.repository.AdminRepository;
import za.ac.cput.guiltfreecookie.service.AdminService;
import za.ac.cput.guiltfreecookie.util.Helper;

// Creating an admin needs an admin, so a brand-new database gets its first one from
// ADMIN_BOOTSTRAP_EMAIL / ADMIN_BOOTSTRAP_PASSWORD. It does nothing once any admin exists.
@Component
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    static final int MIN_PASSWORD_LENGTH = 12;

    private final AdminRepository adminRepository;
    private final AdminService adminService;
    private final String email;
    private final String password;

    public AdminBootstrap(AdminRepository adminRepository, AdminService adminService,
                          @Value("${app.bootstrap.admin-email:}") String email,
                          @Value("${app.bootstrap.admin-password:}") String password) {
        this.adminRepository = adminRepository;
        this.adminService = adminService;
        this.email = Helper.cleanEmail(email);
        this.password = password;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(0)
    public void createFirstAdmin() {
        if (adminRepository.count() > 0 || Helper.isNullOrEmpty(email)) {
            return;
        }
        if (!Helper.isValidEmail(email) || password == null || password.length() < MIN_PASSWORD_LENGTH) {
            log.warn("No admin created: ADMIN_BOOTSTRAP_EMAIL must be valid and ADMIN_BOOTSTRAP_PASSWORD at least {} characters",
                    MIN_PASSWORD_LENGTH);
            return;
        }
        adminService.create(new Admin.Builder()
                .setFirstName("Shop")
                .setLastName("Owner")
                .setEmail(email)
                .setPassword(password)
                .setActive(true)
                .build());
        log.info("Created the first admin ({}). Sign in, change the password, then remove ADMIN_BOOTSTRAP_PASSWORD.", email);
    }
}
