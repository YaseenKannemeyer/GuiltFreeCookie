package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.domain.Admin;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.AdminRepository;
import za.ac.cput.guiltfreecookie.service.notification.EmailService;
import za.ac.cput.guiltfreecookie.service.notification.EmailTemplates;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/email-previews")
@Tag(name = "Admin email previews", description = "See every email with sample data. Admin token required.")
public class EmailPreviewController {

    private final EmailTemplates emailTemplates;
    private final EmailService emailService;
    private final AdminRepository adminRepository;

    public EmailPreviewController(EmailTemplates emailTemplates, EmailService emailService,
                                  AdminRepository adminRepository) {
        this.emailTemplates = emailTemplates;
        this.emailService = emailService;
        this.adminRepository = adminRepository;
    }

    @GetMapping
    @Operation(summary = "Template names")
    public ResponseEntity<List<String>> list() {
        return ResponseEntity.ok(EmailTemplates.NAMES);
    }

    @GetMapping(value = "/{template}", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Render a template with sample data", description = "Show it in a sandboxed iframe.")
    public ResponseEntity<String> preview(@PathVariable String template) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(emailTemplates.render(template, emailTemplates.sample(template)));
    }

    // The recipient is always the signed-in admin, never something the request chooses.
    @PostMapping("/{template}/send-test")
    @Operation(summary = "Email the sample to yourself")
    public ResponseEntity<Void> sendTest(@PathVariable String template, @AuthenticationPrincipal String adminId) {
        Admin admin = adminRepository.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found"));
        emailService.sendTemplate(admin.getEmail(), "[Preview] " + template, template, emailTemplates.sample(template), false);
        return ResponseEntity.accepted().build();
    }
}
