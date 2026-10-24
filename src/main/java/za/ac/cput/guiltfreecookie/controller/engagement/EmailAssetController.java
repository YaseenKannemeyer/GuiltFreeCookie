package za.ac.cput.guiltfreecookie.controller.engagement;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.service.notification.EmailAssetService;

@RestController
@Tag(name = "Email assets")
public class EmailAssetController {

    private final EmailAssetService assets;

    public EmailAssetController(EmailAssetService assets) {
        this.assets = assets;
    }

    @GetMapping(
        value = "/api/v1/email-assets/cookies/{cookieId}.png",
        produces = MediaType.IMAGE_PNG_VALUE
    )
    @Operation(summary = "Email-safe cookie photograph on a cream tile")
    public ResponseEntity<Resource> cookie(@PathVariable String cookieId) {
        var file = assets.image(cookieId);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
            .eTag(file.getFileName().toString())
            .body(new FileSystemResource(file));
    }
}
