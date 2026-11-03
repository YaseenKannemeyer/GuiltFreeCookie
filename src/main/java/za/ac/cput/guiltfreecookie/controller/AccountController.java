package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.domain.Customer;
import za.ac.cput.guiltfreecookie.dto.auth.CustomerAuthResponse;
import za.ac.cput.guiltfreecookie.dto.auth.ProfileRequest;
import za.ac.cput.guiltfreecookie.service.CustomerAuthService;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "My account", description = "Requires a customer token.")
public class AccountController {

    private final CustomerAuthService authService;

    public AccountController(CustomerAuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    @Operation(summary = "My profile")
    public ResponseEntity<Customer> me(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(authService.getProfile(email));
    }

    @PutMapping
    @Operation(summary = "Update my name and mobile number")
    public ResponseEntity<Customer> update(@AuthenticationPrincipal String email, @RequestBody ProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(email, request));
    }

    @PutMapping("/password")
    @Operation(summary = "Change my password", description = "Signs out other sessions and returns a fresh token.")
    public ResponseEntity<CustomerAuthResponse> changePassword(
            @AuthenticationPrincipal String email, @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(
                email, request.getCurrentPassword(), request.getNewPassword()));
    }
}
