package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.ac.cput.guiltfreecookie.dto.engagement.SiteInfo;
import za.ac.cput.guiltfreecookie.service.engagement.SiteInfoService;

@RestController
@RequestMapping("/api/v1/site")
@Tag(name = "Site info", description = "Contact details and trust stats for the homepage, footer and contact page.")
public class SiteInfoController {

    private final SiteInfoService siteInfoService;

    public SiteInfoController(SiteInfoService siteInfoService) {
        this.siteInfoService = siteInfoService;
    }

    @GetMapping("/info")
    @Operation(summary = "Shop contact details and approved-review stats")
    public ResponseEntity<SiteInfo> info() {
        return ResponseEntity.ok(siteInfoService.info());
    }
}
