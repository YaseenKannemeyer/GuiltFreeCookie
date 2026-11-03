package za.ac.cput.guiltfreecookie.service.flow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.EmailFlowSettings;
import za.ac.cput.guiltfreecookie.dto.flow.EmailFlowSettingsRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.repository.EmailFlowSettingsRepository;

import java.time.LocalDateTime;

@Service
public class EmailFlowSettingsService {

    private final EmailFlowSettingsRepository repository;
    private final EmailFlowSettings defaults;

    public EmailFlowSettingsService(
            EmailFlowSettingsRepository repository,
            @Value("${app.flows.enabled:true}") boolean enabled,
            @Value("${app.flows.welcome-discount-percent:10}") int welcomeDiscountPercent,
            @Value("${app.flows.welcome-code-days:5}") int welcomeCodeDays,
            @Value("${app.flows.cart-discount-percent:10}") int cartDiscountPercent,
            @Value("${app.flows.winback-discount-percent:15}") int winbackDiscountPercent,
            @Value("${app.flows.replenishment-days:21}") int replenishmentDays,
            @Value("${app.flows.winback-days:60}") int winbackDays,
            @Value("${app.flows.sunset-days:90}") int sunsetDays) {
        this.repository = repository;
        this.defaults = new EmailFlowSettings.Builder()
                .setSettingsId(EmailFlowSettings.DEFAULT_ID)
                .setWelcomeEnabled(enabled)
                .setBrowseEnabled(enabled)
                .setCartEnabled(enabled)
                .setPostPurchaseEnabled(enabled)
                .setReplenishmentEnabled(enabled)
                .setWinbackEnabled(enabled)
                .setSunsetEnabled(enabled)
                .setWelcomeDiscountPercent(welcomeDiscountPercent)
                .setWelcomeCodeDays(welcomeCodeDays)
                .setCartDiscountPercent(cartDiscountPercent)
                .setWinbackDiscountPercent(winbackDiscountPercent)
                .setReplenishmentDays(replenishmentDays)
                .setWinbackDays(winbackDays)
                .setSunsetDays(sunsetDays)
                .build();
    }

    @Transactional
    public EmailFlowSettings get() {
        return repository.findById(EmailFlowSettings.DEFAULT_ID).orElseGet(() -> repository.save(
                new EmailFlowSettings.Builder().copy(defaults).setUpdatedAt(LocalDateTime.now()).build()));
    }

    // Missing fields keep their current value. A percent of 0 sends the email without a code.
    @Transactional
    public EmailFlowSettings update(EmailFlowSettingsRequest request) {
        EmailFlowSettings existing = get();
        return repository.save(new EmailFlowSettings.Builder()
                .copy(existing)
                .setWelcomeEnabled(flag(request.welcomeEnabled(), existing.isWelcomeEnabled()))
                .setBrowseEnabled(flag(request.browseEnabled(), existing.isBrowseEnabled()))
                .setCartEnabled(flag(request.cartEnabled(), existing.isCartEnabled()))
                .setPostPurchaseEnabled(flag(request.postPurchaseEnabled(), existing.isPostPurchaseEnabled()))
                .setReplenishmentEnabled(flag(request.replenishmentEnabled(), existing.isReplenishmentEnabled()))
                .setWinbackEnabled(flag(request.winbackEnabled(), existing.isWinbackEnabled()))
                .setSunsetEnabled(flag(request.sunsetEnabled(), existing.isSunsetEnabled()))
                .setWelcomeDiscountPercent(number(request.welcomeDiscountPercent(), existing.getWelcomeDiscountPercent(), 0, 50, "Welcome discount"))
                .setWelcomeCodeDays(number(request.welcomeCodeDays(), existing.getWelcomeCodeDays(), 1, 60, "Welcome code days"))
                .setCartDiscountPercent(number(request.cartDiscountPercent(), existing.getCartDiscountPercent(), 0, 50, "Cart discount"))
                .setWinbackDiscountPercent(number(request.winbackDiscountPercent(), existing.getWinbackDiscountPercent(), 0, 50, "Winback discount"))
                .setReplenishmentDays(number(request.replenishmentDays(), existing.getReplenishmentDays(), 3, 180, "Refill reminder days"))
                .setWinbackDays(number(request.winbackDays(), existing.getWinbackDays(), 14, 365, "Winback days"))
                .setSunsetDays(number(request.sunsetDays(), existing.getSunsetDays(), 30, 365, "Sunset days"))
                .setUpdatedAt(LocalDateTime.now())
                .build());
    }

    private static boolean flag(Boolean value, boolean current) {
        return value == null ? current : value;
    }

    private static int number(Integer value, int current, int min, int max, String field) {
        if (value == null) {
            return current;
        }
        if (value < min || value > max) {
            throw new BadRequestException(field + " must be between " + min + " and " + max);
        }
        return value;
    }
}
