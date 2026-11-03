package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.dto.flow.EmailLinkRequest;
import za.ac.cput.guiltfreecookie.service.flow.EmailLinkService;
import za.ac.cput.guiltfreecookie.service.flow.EmailTrackingService;

import java.net.URI;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/email")
@Tag(name = "Email links", description = "Open/click tracking and unsubscribe for flow emails. Links are signed per address.")
public class EmailController {

    private static final byte[] PIXEL = Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    private final EmailTrackingService trackingService;
    private final EmailLinkService linkService;

    public EmailController(EmailTrackingService trackingService, EmailLinkService linkService) {
        this.trackingService = trackingService;
        this.linkService = linkService;
    }

    @GetMapping(value = "/o/{sendId}.gif", produces = MediaType.IMAGE_GIF_VALUE)
    @Operation(summary = "Open pixel")
    public ResponseEntity<byte[]> open(@PathVariable String sendId) {
        trackingService.opened(sendId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(PIXEL);
    }

    @GetMapping("/c/{sendId}")
    @Operation(summary = "Tracked link", description = "Redirects to the storefront page in u; anything else goes to the home page.")
    public ResponseEntity<Void> click(@PathVariable String sendId, @RequestParam(name = "u", required = false) String target) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .cacheControl(CacheControl.noStore())
                .location(URI.create(trackingService.clicked(sendId, target)))
                .build();
    }

    @PostMapping("/unsubscribe")
    @Operation(summary = "Unsubscribe from all marketing email", description = "Order emails still arrive.")
    public ResponseEntity<Void> unsubscribe(@RequestBody EmailLinkRequest request) {
        linkService.unsubscribe(request.email(), request.token(), "Unsubscribe page");
        return ResponseEntity.noContent().build();
    }

    // Mail apps call this from the List-Unsubscribe header (RFC 8058), with no page in between.
    @PostMapping("/unsubscribe/one-click")
    @Operation(summary = "One-click unsubscribe (List-Unsubscribe-Post)")
    public ResponseEntity<Void> oneClick(@RequestParam("e") String email, @RequestParam("t") String token) {
        linkService.unsubscribe(email, token, "One-click");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resubscribe")
    @Operation(summary = "Undo an unsubscribe")
    public ResponseEntity<Void> resubscribe(@RequestBody EmailLinkRequest request) {
        linkService.resubscribe(request.email(), request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/stay")
    @Operation(summary = "Keep me subscribed", description = "From the re-engagement emails.")
    public ResponseEntity<Void> stay(@RequestBody EmailLinkRequest request) {
        linkService.stay(request.email(), request.token());
        return ResponseEntity.noContent().build();
    }
}
