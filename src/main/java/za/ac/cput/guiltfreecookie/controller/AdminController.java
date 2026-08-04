package za.ac.cput.guiltfreecookie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import za.ac.cput.guiltfreecookie.domain.Admin;

import za.ac.cput.guiltfreecookie.service.AdminService;

import java.util.List;

@RestController
@RequestMapping("/admin")
@CrossOrigin
public class AdminController {

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/create")
    public ResponseEntity<Admin> create(@RequestBody Admin admin) {
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
        return ResponseEntity.ok(adminService.update(admin));
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

        return ResponseEntity.ok(foundAdmin);
    }
}