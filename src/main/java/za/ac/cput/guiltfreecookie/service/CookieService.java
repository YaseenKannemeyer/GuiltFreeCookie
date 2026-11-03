package za.ac.cput.guiltfreecookie.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.service.catalog.CookieImageService;
import za.ac.cput.guiltfreecookie.service.catalog.CookieVariantService;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CookieService implements IService<Cookie, String> {

    private final CookieRepository cookieRepository;
    private final CookieVariantService variantService;
    private final CookieImageService imageService;

    @Autowired
    public CookieService(CookieRepository cookieRepository, CookieVariantService variantService,
                         CookieImageService imageService) {
        this.cookieRepository = cookieRepository;
        this.variantService = variantService;
        this.imageService = imageService;
    }

    // Every new cookie gets a slug and a "Single" pack so it can be sold straight away.
    @Override
    @Transactional
    public Cookie create(Cookie cookie) {
        Cookie toSave = new Cookie.Builder()
                .copy(cookie)
                .setSlug(cookie.getSlug() != null ? cookie.getSlug() : uniqueSlug(cookie.getDescription(), null))
                .setCreatedAt(cookie.getCreatedAt() != null ? cookie.getCreatedAt() : LocalDateTime.now())
                .build();
        Cookie created = this.cookieRepository.save(toSave);
        variantService.ensureDefault(created);
        return created;
    }

    @Override
    public Cookie read(String id) {
        return this.cookieRepository.findById(id).orElse(null);
    }

    @Override
    public Cookie update(Cookie cookie) {

        Cookie existing = cookieRepository.findById(cookie.getCookieId()).orElse(null);

        if (existing == null) {
            return null;
        }

        Cookie updated = new Cookie.Builder()
                .copy(existing)
                .setCategory(cookie.getCategory())
                .setDescription(cookie.getDescription())
                .setLongDescription(cookie.getLongDescription())
                .setIngredients(cookie.getIngredients())
                .setAllergies(cookie.getAllergies())
                .setPrice(cookie.getPrice())
                .setImage(cookie.getImage())
                .build();

        return cookieRepository.save(updated);
    }

    @Override
    @Transactional
    public boolean delete(String id) {
        if (this.cookieRepository.existsById(id)) {
            variantService.deleteAllFor(id);
            imageService.deleteAllFor(id);
            this.cookieRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public List<Cookie> getAll() {
        return this.cookieRepository.findAll();
    }

    public List<Cookie> getAllActive() {
        return this.cookieRepository.findByArchivedFalse();
    }

    public Cookie setArchived(String id, boolean archived) {
        Cookie existing = cookieRepository.findById(id).orElse(null);

        if (existing == null) {
            return null;
        }

        Cookie updated = new Cookie.Builder()
                .copy(existing)
                .setArchived(archived)
                .build();

        return cookieRepository.save(updated);
    }

    public String uniqueSlug(String text, String cookieId) {
        String base = Helper.slugify(text);
        if (base.isEmpty()) {
            base = "cookie";
        }
        String slug = base;
        for (int n = 2; isTaken(slug, cookieId); n++) {
            slug = base + "-" + n;
        }
        return slug;
    }

    private boolean isTaken(String slug, String cookieId) {
        return cookieRepository.findBySlug(slug)
                .filter(other -> !other.getCookieId().equals(cookieId))
                .isPresent();
    }
}
