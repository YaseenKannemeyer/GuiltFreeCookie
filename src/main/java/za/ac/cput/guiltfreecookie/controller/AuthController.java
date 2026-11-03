package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.dto.auth.CustomerAuthResponse;
import za.ac.cput.guiltfreecookie.dto.auth.ForgotPasswordRequest;
import za.ac.cput.guiltfreecookie.dto.auth.LoginRequest;
import za.ac.cput.guiltfreecookie.dto.auth.RegisterRequest;
import za.ac.cput.guiltfreecookie.dto.auth.ResetPasswordRequest;
import za.ac.cput.guiltfreecookie.service.CustomerAuthService;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Customer auth", description = "Optional customer accounts. Checkout never requires one.")
public class AuthController {

    private final CustomerAuthService authService;

    public AuthController(CustomerAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Create an account and sign in")
    public ResponseEntity<CustomerAuthResponse> register(@RequestBody RegisterRequest request) {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in with email and password")
    public ResponseEntity<CustomerAuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Email a password reset link", description = "Always 204, whether or not the email has an account.")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set a new password from an emailed token and sign in")
    public ResponseEntity<CustomerAuthResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
