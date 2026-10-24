package za.ac.cput.guiltfreecookie.service.engagement;

import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class SiteInfoService {

    private final Environment env;
    private final ReviewService reviews;

    public SiteInfoService(Environment env, ReviewService reviews) {
        this.env = env;
        this.reviews = reviews;
    }

    public String setting(String name) {
        return env.getProperty("app.site." + name, "");
    }

    public Map<String, Object> info() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("supportEmail", setting("support-email"));
        result.put("phone", setting("phone"));
        result.put("whatsapp", setting("whatsapp"));
        result.put("hours", setting("hours"));
        result.put("responseTime", setting("response-time"));
        result.put("address", setting("address"));
        result.put(
            "social",
            Map.of(
                "instagram",
                setting("instagram"),
                "facebook",
                setting("facebook"),
                "tiktok",
                setting("tiktok")
            )
        );
        var rating = reviews.overall();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("averageRating", rating.averageRating());
        stats.put("reviewCount", rating.reviewCount());
        stats.put("customersServed", null);
        stats.put("cookiesSold", null);
        result.put("stats", stats);
        return result;
    }
}
