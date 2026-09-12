package za.ac.cput.guiltfreecookie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import za.ac.cput.guiltfreecookie.domain.Admin;

import za.ac.cput.guiltfreecookie.security.JwtService;
import za.ac.cput.guiltfreecookie.service.AdminService;
import za.ac.cput.guiltfreecookie.service.PasswordResetResult;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AdminController(AdminService adminService, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.adminService = adminService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/create")
    public ResponseEntity<Admin> create(@RequestBody Admin admin) {
        if (Helper.isNullOrEmpty(admin.getFirstName())
                || Helper.isNullOrEmpty(admin.getLastName())
                || Helper.isNullOrEmpty(admin.getPassword())
                || !Helper.isValidEmail(admin.getEmail())) {
            return ResponseEntity.badRequest().build();
        }

        return new ResponseEntity<>(adminService.create(admin), HttpStatus.CREATED);
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<Admin>> getAll() {
        return ResponseEntity.ok(adminService.getAll());
    }

    @GetMapping("/read/{id}")
    public ResponseEntity<Admin> read(@PathVariable String id) {
        Admin admin = adminService.read(id);

        if (admin == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        return ResponseEntity.ok(admin);
    }

    @PutMapping("/update")
    public ResponseEntity<Admin> update(@RequestBody Admin admin) {
        if (Helper.isNullOrEmpty(admin.getFirstName())
                || Helper.isNullOrEmpty(admin.getLastName())
                || !Helper.isValidEmail(admin.getEmail())) {
            return ResponseEntity.badRequest().build();
        }

        Admin updated = adminService.update(admin);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        adminService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Admin admin) {

        Admin foundAdmin = adminService.login(admin.getEmail(), admin.getPassword());

        if (foundAdmin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }

        String token = jwtService.generateToken(foundAdmin.getAdminId(), foundAdmin.getEmail());
        return ResponseEntity.ok(new LoginResponse(foundAdmin, token));
    }

    @PutMapping("/active/{id}")
    public ResponseEntity<Admin> setActive(@PathVariable String id, @RequestParam boolean active) {
        Admin updated = adminService.setActive(id, active);

        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updated);
    }

    @PostMapping("/resetPassword/{id}")
    public ResponseEntity<PasswordResetResult> resetPassword(@PathVariable String id) {
        PasswordResetResult result = adminService.resetPassword(id);

        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result);
    }

    @PutMapping("/changePassword/{id}")
    public ResponseEntity<?> changePassword(
            @PathVariable String id,
            @RequestBody ChangePasswordRequest request) {

        Admin admin = adminService.read(id);
        if (admin == null) {
            return ResponseEntity.notFound().build();
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Current password is incorrect");
        }

        if (Helper.isNullOrEmpty(request.getNewPassword())) {
            return ResponseEntity.badRequest().body("New password cannot be empty");
        }

        Admin updated = adminService.changePassword(id, request.getNewPassword());
        return ResponseEntity.ok(updated);
    }
}
