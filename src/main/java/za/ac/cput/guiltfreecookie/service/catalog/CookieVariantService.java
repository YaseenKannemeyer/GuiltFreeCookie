package za.ac.cput.guiltfreecookie.service.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.dto.catalog.VariantRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CookieVariantRepository;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CookieVariantService {

    static final String DEFAULT_LABEL = "Single";
    static final int MAX_LABEL_LENGTH = 50;
    static final int MAX_UNITS_PER_PACK = 100;
    static final BigDecimal MAX_PRICE = BigDecimal.valueOf(100_000);

    private final CookieVariantRepository variantRepository;
    private final CookieRepository cookieRepository;

    public CookieVariantService(CookieVariantRepository variantRepository, CookieRepository cookieRepository) {
        this.variantRepository = variantRepository;
        this.cookieRepository = cookieRepository;
    }

    public List<CookieVariant> list(String cookieId) {
        requireCookie(cookieId);
        return variantRepository.findByCookieIdOrderBySortOrderAscUnitsPerPackAsc(cookieId);
    }

    public CookieVariant create(String cookieId, VariantRequest request) {
        requireCookie(cookieId);
        String label = validLabel(request.label());
        if (variantRepository.existsByCookieIdAndLabelIgnoreCase(cookieId, label)) {
            throw new ConflictException("This cookie already has a '" + label + "' pack");
        }
        int units = validUnits(request.unitsPerPack());
        return variantRepository.save(new CookieVariant.Builder()
                .setCookieId(cookieId)
                .setLabel(label)
                .setUnitsPerPack(units)
                .setPrice(validPrice(request.price()))
                .setActive(request.active() == null || request.active())
                .setSortOrder(request.sortOrder() != null ? request.sortOrder() : units)
                .build());
    }

    public CookieVariant update(String cookieId, String variantId, VariantRequest request) {
        CookieVariant existing = requireVariant(cookieId, variantId);
        String label = validLabel(request.label());
        if (!label.equalsIgnoreCase(existing.getLabel())
                && variantRepository.existsByCookieIdAndLabelIgnoreCase(cookieId, label)) {
            throw new ConflictException("This cookie already has a '" + label + "' pack");
        }
        return variantRepository.save(new CookieVariant.Builder()
                .copy(existing)
                .setLabel(label)
                .setUnitsPerPack(validUnits(request.unitsPerPack()))
                .setPrice(validPrice(request.price()))
                .setActive(request.active() == null || request.active())
                .setSortOrder(request.sortOrder() != null ? request.sortOrder() : existing.getSortOrder())
                .build());
    }

    // Packs are never hard-deleted: carts and past orders may point at them.
    public CookieVariant deactivate(String cookieId, String variantId) {
        CookieVariant existing = requireVariant(cookieId, variantId);
        return variantRepository.save(new CookieVariant.Builder().copy(existing).setActive(false).build());
    }

    @Transactional
    public void ensureDefault(Cookie cookie) {
        if (!variantRepository.existsByCookieId(cookie.getCookieId())) {
            variantRepository.save(new CookieVariant.Builder()
                    .setCookieId(cookie.getCookieId())
                    .setLabel(DEFAULT_LABEL)
                    .setUnitsPerPack(1)
                    .setSortOrder(1)
                    .build());
        }
    }

    @Transactional
    public void deleteAllFor(String cookieId) {
        variantRepository.deleteByCookieId(cookieId);
    }

    private void requireCookie(String cookieId) {
        if (!cookieRepository.existsById(cookieId)) {
            throw new NotFoundException("Cookie not found");
        }
    }

    private CookieVariant requireVariant(String cookieId, String variantId) {
        return variantRepository.findById(variantId)
                .filter(variant -> variant.getCookieId().equals(cookieId))
                .orElseThrow(() -> new NotFoundException("Pack size not found"));
    }

    private static String validLabel(String label) {
        String cleaned = Helper.clean(label);
        if (!Helper.isPresent(cleaned, MAX_LABEL_LENGTH)) {
            throw new BadRequestException("Give the pack a name of up to " + MAX_LABEL_LENGTH + " characters");
        }
        return cleaned;
    }

    private static int validUnits(Integer units) {
        if (units == null || units < 1 || units > MAX_UNITS_PER_PACK) {
            throw new BadRequestException("Cookies per pack must be between 1 and " + MAX_UNITS_PER_PACK);
        }
        return units;
    }

    private static BigDecimal validPrice(BigDecimal price) {
        if (price == null) {
            return null;
        }
        if (price.signum() <= 0 || price.compareTo(MAX_PRICE) > 0 || price.stripTrailingZeros().scale() > 2) {
            throw new BadRequestException("Price must be between 0.01 and " + MAX_PRICE + ", in rand and cents");
        }
        return price;
    }
}
