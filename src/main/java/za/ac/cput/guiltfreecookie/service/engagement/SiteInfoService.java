package za.ac.cput.guiltfreecookie.service.engagement;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.dto.engagement.RatingSummary;
import za.ac.cput.guiltfreecookie.dto.engagement.SiteInfo;

// Shop identity from app.site.* plus live review stats. Email templates call setting() directly.
@Service
public class SiteInfoService {

    private final Environment environment;
    private final ReviewService reviewService;

    public SiteInfoService(Environment environment, ReviewService reviewService) {
        this.environment = environment;
        this.reviewService = reviewService;
    }

    public String setting(String name) {
        return environment.getProperty("app.site." + name, "");
    }

    public SiteInfo info() {
        RatingSummary rating = reviewService.overall();
        return new SiteInfo(
                setting("support-email"),
                setting("phone"),
                setting("whatsapp"),
                setting("hours"),
                setting("response-time"),
                setting("address"),
                new SiteInfo.Social(setting("instagram"), setting("facebook"), setting("tiktok")),
                new SiteInfo.Stats(rating.averageRating(), rating.reviewCount(), null, null));
    }
}
