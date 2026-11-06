package za.ac.cput.guiltfreecookie.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import za.ac.cput.guiltfreecookie.domain.*;
import za.ac.cput.guiltfreecookie.repository.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Only seeds the demo database. Existing products and inventory are never overwritten. */
@Component
@Profile({"demo", "showcase"})
public class DemoData {
    private final CookieRepository cookies;
    private final CookieVariantRepository variants;
    private final DiscountCodeRepository discounts;
    private final Path uploads;

    public DemoData(CookieRepository cookies, CookieVariantRepository variants, DiscountCodeRepository discounts,
                    Environment environment, @Value("${app.upload.root-dir}") String uploads) {
        if (environment.acceptsProfiles(Profiles.of("demo")) && environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("The demo and prod profiles must never run together");
        }
        if (environment.acceptsProfiles(Profiles.of("showcase")) && !environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("The hosted showcase profile requires prod configuration");
        }
        this.cookies = cookies;
        this.variants = variants;
        this.discounts = discounts;
        this.uploads = Path.of(uploads);
    }

    // R5 a cookie, and the bigger the box the more you save: 10%, 20%, 30%. scripts/reprice.sh uses the same prices.
    static final double SINGLE_PRICE = 5.0;
    static final Map<Integer, BigDecimal> BOXES = new TreeMap<>(Map.of(
            6, new BigDecimal("27.00"), 12, new BigDecimal("48.00"), 24, new BigDecimal("84.00")));

    record Sample(String name, String slug, CookieCategory category, String description,
                  String proposedIngredients, String proposedAllergens, double price) { }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() throws IOException {
        List<Sample> samples;
        try (var input = new ClassPathResource("demo/catalog.json").getInputStream()) {
            samples = new ObjectMapper().readValue(input, new TypeReference<>() { });
        }
        Files.createDirectories(uploads.resolve("cookies"));
        for (Sample sample : samples) {
            Path image = uploads.resolve("cookies/" + sample.slug() + ".webp");
            if (!Files.exists(image)) {
                try (var input = new ClassPathResource("demo/cookies/" + sample.slug() + ".webp").getInputStream()) {
                    Files.copy(input, image);
                }
            }
            if (cookies.existsBySlug(sample.slug())) continue;
            Cookie cookie = cookies.save(new Cookie.Builder().setDescription(sample.name()).setSlug(sample.slug())
                    .setCategory(sample.category()).setLongDescription(sample.description())
                    .setIngredients("Sample recipe: " + sample.proposedIngredients())
                    .setAllergies("Illustrative allergens: " + sample.proposedAllergens())
                    .setPrice(SINGLE_PRICE).setStockQuantity(120).setFeatured(true)
                    .setImage("cookies/" + sample.slug() + ".webp").setCreatedAt(LocalDateTime.now()).build());
            variants.save(new CookieVariant.Builder().setCookieId(cookie.getCookieId()).setLabel("Single").build());
            int sortOrder = 1;
            for (var box : BOXES.entrySet()) {
                variants.save(new CookieVariant.Builder().setCookieId(cookie.getCookieId()).setLabel("Box of " + box.getKey())
                        .setUnitsPerPack(box.getKey()).setPrice(box.getValue()).setSortOrder(sortOrder++).build());
            }
        }
        if (!discounts.existsByCode("DEMO10")) {
            discounts.save(new DiscountCode.Builder().setCode("DEMO10").setType(DiscountType.PERCENT)
                    .setValue(BigDecimal.TEN).setActive(true).setCreatedAt(LocalDateTime.now()).build());
        }
    }
}
