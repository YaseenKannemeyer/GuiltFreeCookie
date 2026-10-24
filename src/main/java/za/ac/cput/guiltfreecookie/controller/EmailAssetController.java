package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import za.ac.cput.guiltfreecookie.service.notification.EmailAssetService;

import java.nio.file.Path;
import java.time.Duration;

@RestController
@Tag(name = "Email assets", description = "Images that work in every email client.")
public class EmailAssetController {

    private final EmailAssetService emailAssetService;

    public EmailAssetController(EmailAssetService emailAssetService) {
        this.emailAssetService = emailAssetService;
    }

    @GetMapping(value = "/api/v1/email-assets/cookies/{cookieId}.png", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "A cookie's photo on an opaque cream tile, for emails")
    public ResponseEntity<Resource> cookie(@PathVariable String cookieId) {
        Path file = emailAssetService.image(cookieId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .eTag(file.getFileName().toString())
                .body(new FileSystemResource(file));
    }
}
