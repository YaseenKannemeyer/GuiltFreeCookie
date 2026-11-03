package za.ac.cput.guiltfreecookie.service.catalog;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.service.CookieService;

import java.time.LocalDateTime;

// Cookies created before Phase 2 get a slug, a created date and a "Single" pack. Safe to run every start.
@Component
public class CatalogBackfill {

    private final CookieRepository cookieRepository;
    private final CookieService cookieService;
    private final CookieVariantService variantService;

    public CatalogBackfill(CookieRepository cookieRepository, CookieService cookieService,
                           CookieVariantService variantService) {
        this.cookieRepository = cookieRepository;
        this.cookieService = cookieService;
        this.variantService = variantService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        for (Cookie cookie : cookieRepository.findAll()) {
            Cookie current = cookie;
            if (cookie.getSlug() == null || cookie.getCreatedAt() == null) {
                current = cookieRepository.save(new Cookie.Builder()
                        .copy(cookie)
                        .setSlug(cookie.getSlug() != null ? cookie.getSlug()
                                : cookieService.uniqueSlug(cookie.getDescription(), cookie.getCookieId()))
                        .setCreatedAt(cookie.getCreatedAt() != null ? cookie.getCreatedAt() : LocalDateTime.now())
                        .build());
            }
            variantService.ensureDefault(current);
        }
    }
}
