package za.ac.cput.guiltfreecookie.controller.engagement;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.AdminRepository;
import za.ac.cput.guiltfreecookie.service.notification.*;

@RestController
@RequestMapping("/api/v1/admin/email-previews")
@Tag(name = "Email previews")
public class EmailPreviewController {

    private final EmailTemplates templates;
    private final EmailService email;
    private final AdminRepository admins;

    public EmailPreviewController(
        EmailTemplates templates,
        EmailService email,
        AdminRepository admins
    ) {
        this.templates = templates;
        this.email = email;
        this.admins = admins;
    }

    @GetMapping
    @Operation(summary = "List email templates")
    public List<String> list() {
        return EmailTemplates.NAMES;
    }

    @GetMapping(value = "/{template}", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Render an email with illustrative sample data")
    public ResponseEntity<String> preview(@PathVariable String template) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(templates.render(template, templates.sample(template)));
    }

    @PostMapping("/{template}/send-test")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Send a sample email to the authenticated admin")
    public void send(
        @PathVariable String template,
        @AuthenticationPrincipal String adminId
    ) {
        var model = templates.sample(template);
        var admin = admins
            .findById(adminId)
            .orElseThrow(() -> new NotFoundException("Admin not found"));
        email.sendTemplate(
            admin.getEmail(),
            "[Preview] " + template,
            template,
            model,
            false
        );
    }
}
