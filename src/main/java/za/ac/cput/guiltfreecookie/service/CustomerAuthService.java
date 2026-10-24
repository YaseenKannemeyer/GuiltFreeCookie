package za.ac.cput.guiltfreecookie.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.exception.UnauthorizedException;
import za.ac.cput.guiltfreecookie.repository.PasswordResetTokenRepository;
import za.ac.cput.guiltfreecookie.security.JwtService;
import za.ac.cput.guiltfreecookie.service.notification.EmailService;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class CustomerAuthService {

    static final int MAX_NAME_LENGTH = 100;
    static final String INVALID_LOGIN = "Invalid email or password";
    static final String INVALID_RESET_LINK = "This reset link is invalid or has expired";

    private final CustomerService customerService;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final NewsletterService newsletterService;
    private final String frontendBaseUrl;
    private final long resetMinutes;
    private final SecureRandom random = new SecureRandom();
    private volatile String dummyHash;

    public CustomerAuthService(
            CustomerService customerService,
            PasswordResetTokenRepository resetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService,
            NewsletterService newsletterService,
            @Value("${app.frontend.base-url}") String frontendBaseUrl,
            @Value("${app.auth.password-reset-minutes}") long resetMinutes) {
        this.customerService = customerService;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.newsletterService = newsletterService;
        this.frontendBaseUrl = frontendBaseUrl;
        this.resetMinutes = resetMinutes;
    }

    // An existing email is a conflict even without a password: the owner proves
    // the inbox is theirs through forgot-password instead.
    @Transactional
    public CustomerAuthResponse register(RegisterRequest request) {
        String email = Helper.cleanEmail(request.email());
        if (!Helper.isValidEmail(email)) {
            throw new BadRequestException("Enter a valid email address");
        }
        requireValidPassword(request.password());
        Name name = cleanName(request.firstName(), request.middleName(), request.lastName());
        String mobile = cleanMobile(request.mobileNumber());

        if (customerService.read(email) != null) {
            throw new ConflictException("An account with this email already exists. Use 'Forgot password' to sign in.");
        }

        Customer created = customerService.create(new Customer.Builder()
                .setCustomerEmail(email)
                .setName(name)
                .setMobileNumber(mobile)
                .setPasswordHash(passwordEncoder.encode(request.password()))
                .setTokenVersion(0)
                .build());

        if (Boolean.TRUE.equals(request.newsletterOptIn())) {
            newsletterService.subscribe(email);
        }
        return new CustomerAuthResponse(created, jwtService.generateToken(created));
    }

    public CustomerAuthResponse login(LoginRequest request) {
        String email = Helper.cleanEmail(request.email());
        Customer customer = email == null ? null : customerService.read(email);

        // Always run one bcrypt check so response time doesn't reveal which emails have accounts.
        String hash = customer != null && customer.hasAccount() ? customer.getPasswordHash() : dummyHash();
        boolean matches = request.password() != null && passwordEncoder.matches(request.password(), hash);

        if (customer == null || !customer.hasAccount() || !matches) {
            throw new UnauthorizedException(INVALID_LOGIN);
        }
        return new CustomerAuthResponse(customer, jwtService.generateToken(customer));
    }

    // Says nothing about whether the email exists.
    @Transactional
    public void forgotPassword(String rawEmail) {
        String email = Helper.cleanEmail(rawEmail);
        if (!Helper.isValidEmail(email)) {
            throw new BadRequestException("Enter a valid email address");
        }
        if (customerService.read(email) == null) {
            return;
        }

        String token = newToken();
        resetTokenRepository.deleteByCustomerEmail(email);
        resetTokenRepository.save(new PasswordResetToken.Builder()
                .setTokenHash(sha256(token))
                .setCustomerEmail(email)
                .setExpiresAt(LocalDateTime.now().plusMinutes(resetMinutes))
                .build());

        String link = frontendBaseUrl + "/reset-password?token=" + token;
        emailService.send(email, "Reset your Guilt Free Cookie password",
                "We received a request to set or reset the password for your Guilt Free Cookie account.\n\n"
                        + "Choose a new password here (the link works once and expires in " + resetMinutes + " minutes):\n"
                        + link + "\n\n"
                        + "If you didn't ask for this, you can ignore this email.");
    }

    @Transactional
    public CustomerAuthResponse resetPassword(ResetPasswordRequest request) {
        if (Helper.isNullOrEmpty(request.token())) {
            throw new BadRequestException(INVALID_RESET_LINK);
        }
        requireValidPassword(request.newPassword());

        PasswordResetToken token = resetTokenRepository.findById(sha256(request.token())).orElse(null);
        if (token == null || token.isExpired(LocalDateTime.now())) {
            throw new BadRequestException(INVALID_RESET_LINK);
        }
        Customer customer = customerService.read(token.getCustomerEmail());
        if (customer == null) {
            throw new BadRequestException(INVALID_RESET_LINK);
        }

        Customer updated = customerService.save(withNewPassword(customer, request.newPassword()));
        resetTokenRepository.deleteByCustomerEmail(customer.getCustomerEmail());
        return new CustomerAuthResponse(updated, jwtService.generateToken(updated));
    }

    public Customer getProfile(String email) {
        Customer customer = customerService.read(email);
        if (customer == null) {
            throw new NotFoundException("Account not found");
        }
        return customer;
    }

    public Customer updateProfile(String email, ProfileRequest request) {
        Customer existing = getProfile(email);
        Customer updated = new Customer.Builder()
                .copy(existing)
                .setName(cleanName(request.firstName(), request.middleName(), request.lastName()))
                .setMobileNumber(cleanMobile(request.mobileNumber()))
                .build();
        return customerService.update(updated);
    }

    // Ends every other session (token version bump), so the caller gets a fresh token.
    public CustomerAuthResponse changePassword(String email, String currentPassword, String newPassword) {
        Customer customer = getProfile(email);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, customer.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        requireValidPassword(newPassword);

        Customer updated = customerService.save(withNewPassword(customer, newPassword));
        return new CustomerAuthResponse(updated, jwtService.generateToken(updated));
    }

    private Customer withNewPassword(Customer customer, String newPassword) {
        return new Customer.Builder()
                .copy(customer)
                .setPasswordHash(passwordEncoder.encode(newPassword))
                .setTokenVersion(customer.getTokenVersion() + 1)
                .build();
    }

    private static void requireValidPassword(String password) {
        if (!Helper.isValidPassword(password)) {
            throw new BadRequestException(
                    "Password must be at least " + Helper.MIN_PASSWORD_LENGTH + " characters");
        }
    }

    private static Name cleanName(String firstName, String middleName, String lastName) {
        Name name = new Name.Builder()
                .setFirstName(Helper.clean(firstName))
                .setMiddleName(Helper.clean(middleName))
                .setLastName(Helper.clean(lastName))
                .build();
        if (!Helper.isPresent(name.getFirstName(), MAX_NAME_LENGTH)
                || !Helper.isPresent(name.getLastName(), MAX_NAME_LENGTH)
                || !Helper.isWithin(name.getMiddleName(), MAX_NAME_LENGTH)) {
            throw new BadRequestException("Enter your first and last name");
        }
        return name;
    }

    private static String cleanMobile(String mobileNumber) {
        String mobile = Helper.clean(mobileNumber);
        if (Helper.isNullOrEmpty(mobile)) {
            return null;
        }
        if (!Helper.isValidMobileNumber(mobile)) {
            throw new BadRequestException("Enter a valid South African mobile number");
        }
        return mobile;
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
        }
        return dummyHash;
    }
}
