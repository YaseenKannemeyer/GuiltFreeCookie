package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.domain.EmailFlowSettings;
import za.ac.cput.guiltfreecookie.dto.flow.EmailFlowSettingsRequest;
import za.ac.cput.guiltfreecookie.dto.flow.FlowStats;
import za.ac.cput.guiltfreecookie.service.flow.EmailFlowSettingsService;
import za.ac.cput.guiltfreecookie.service.flow.FlowStatsService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/email-flows")
@Tag(name = "Admin email flows", description = "Switch the automated emails on or off, tune them, and see how they do. Admin token required.")
public class AdminEmailFlowController {

    private final EmailFlowSettingsService settingsService;
    private final FlowStatsService statsService;

    public AdminEmailFlowController(EmailFlowSettingsService settingsService, FlowStatsService statsService) {
        this.settingsService = settingsService;
        this.statsService = statsService;
    }

    @GetMapping
    @Operation(summary = "Current flow settings")
    public ResponseEntity<EmailFlowSettings> get() {
        return ResponseEntity.ok(settingsService.get());
    }

    @PutMapping
    @Operation(summary = "Update flow settings", description = "Missing fields keep their value. A discount of 0 sends no code.")
    public ResponseEntity<EmailFlowSettings> update(@RequestBody EmailFlowSettingsRequest request) {
        return ResponseEntity.ok(settingsService.update(request));
    }

    @GetMapping("/stats")
    @Operation(summary = "Sent, opened and clicked per flow", description = "Opens are approximate (mail apps preload images).")
    public ResponseEntity<List<FlowStats>> stats(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(statsService.since(days));
    }
}
