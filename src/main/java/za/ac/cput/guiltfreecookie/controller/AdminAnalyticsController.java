package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.ac.cput.guiltfreecookie.dto.analytics.AnalyticsSummary;
import za.ac.cput.guiltfreecookie.service.analytics.AnalyticsService;
import za.ac.cput.guiltfreecookie.service.flow.FlowEnrollmentService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@Tag(name = "Admin analytics", description = "Sales, customers, checkout funnel and email results. Admin token required.")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    public AdminAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @Operation(summary = "Overview for a period", description = "from and to are inclusive dates (yyyy-MM-dd) in shop time, "
            + "at most 366 days apart. Defaults to the last 30 days. Compared with the same number of days just before.")
    public ResponseEntity<AnalyticsSummary> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now(FlowEnrollmentService.SHOP_ZONE);
        LocalDate start = from != null ? from : end.minusDays(29);
        return ResponseEntity.ok(analyticsService.summary(start, end));
    }
}
