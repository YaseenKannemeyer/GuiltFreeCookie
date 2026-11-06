package za.ac.cput.guiltfreecookie.service.shipping;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.ShippingSettings;
import za.ac.cput.guiltfreecookie.dto.shipping.ShippingSettingsRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.repository.ShippingSettingsRepository;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ShippingSettingsService {

    static final BigDecimal MAX_AMOUNT = BigDecimal.valueOf(10_000);
    static final int MAX_ETA_LENGTH = 100;
    static final int MAX_ADDRESS_LENGTH = 255;
    static final int MAX_INSTRUCTIONS_LENGTH = 500;

    private final ShippingSettingsRepository repository;
    private final ShippingSettings defaults;

    public ShippingSettingsService(
            ShippingSettingsRepository repository,
            @Value("${app.shipping.courier-rate}") BigDecimal courierRate,
            @Value("${app.shipping.pudo-rate}") BigDecimal pudoRate,
            @Value("${app.shipping.pudo-enabled:true}") boolean pudoEnabled,
            @Value("${app.shipping.free-threshold}") BigDecimal freeThreshold,
            @Value("${app.shipping.pickup-address}") String pickupAddress,
            @Value("${app.shipping.pickup-instructions}") String pickupInstructions) {
        this.repository = repository;
        this.defaults = new ShippingSettings.Builder()
                .setSettingsId(ShippingSettings.DEFAULT_ID)
                .setCourierEnabled(true)
                .setCourierRate(courierRate)
                .setCourierEta("2–4 business days")
                .setPudoEnabled(pudoEnabled)
                .setPudoRate(pudoRate)
                .setPudoEta("2–4 business days to your locker")
                .setPickupEnabled(true)
                .setPickupAddress(pickupAddress)
                .setPickupInstructions(pickupInstructions)
                .setPickupEta("Ready within 1 business day")
                .setFreeShippingThreshold(freeThreshold)
                .build();
    }

    @Transactional
    public ShippingSettings get() {
        return repository.findById(ShippingSettings.DEFAULT_ID).orElseGet(() -> repository.save(
                new ShippingSettings.Builder().copy(defaults).setUpdatedAt(LocalDateTime.now()).build()));
    }

    @Transactional
    public ShippingSettings update(ShippingSettingsRequest request) {
        ShippingSettings existing = get();
        boolean courier = request.courierEnabled() == null ? existing.isCourierEnabled() : request.courierEnabled();
        boolean pudo = request.pudoEnabled() == null ? existing.isPudoEnabled() : request.pudoEnabled();
        boolean pickup = request.pickupEnabled() == null ? existing.isPickupEnabled() : request.pickupEnabled();
        if (!courier && !pudo && !pickup) {
            throw new BadRequestException("Keep at least one delivery option switched on");
        }
        String pickupAddress = text(request.pickupAddress(), MAX_ADDRESS_LENGTH, "Pickup address");
        if (pickup && Helper.isNullOrEmpty(pickupAddress)) {
            throw new BadRequestException("Local pickup needs a pickup address");
        }
        return repository.save(new ShippingSettings.Builder()
                .copy(existing)
                .setCourierEnabled(courier)
                .setCourierRate(amount(request.courierRate(), "Courier rate", false))
                .setCourierEta(text(request.courierEta(), MAX_ETA_LENGTH, "Courier delivery time"))
                .setPudoEnabled(pudo)
                .setPudoRate(amount(request.pudoRate(), "PUDO rate", false))
                .setPudoEta(text(request.pudoEta(), MAX_ETA_LENGTH, "PUDO delivery time"))
                .setPickupEnabled(pickup)
                .setPickupAddress(pickupAddress)
                .setPickupInstructions(text(request.pickupInstructions(), MAX_INSTRUCTIONS_LENGTH, "Pickup instructions"))
                .setPickupEta(text(request.pickupEta(), MAX_ETA_LENGTH, "Pickup time"))
                .setFreeShippingThreshold(amount(request.freeShippingThreshold(), "Free delivery threshold", true))
                .setUpdatedAt(LocalDateTime.now())
                .build());
    }

    private static BigDecimal amount(BigDecimal value, String field, boolean optional) {
        if (value == null) {
            if (optional) {
                return null;
            }
            throw new BadRequestException(field + " is required");
        }
        if (value.signum() < 0 || value.compareTo(MAX_AMOUNT) > 0 || value.stripTrailingZeros().scale() > 2) {
            throw new BadRequestException(field + " must be a rand amount between 0 and " + MAX_AMOUNT);
        }
        return value;
    }

    private static String text(String value, int max, String field) {
        String cleaned = Helper.clean(value);
        if (!Helper.isWithin(cleaned, max)) {
            throw new BadRequestException(field + " must be at most " + max + " characters");
        }
        return Helper.isNullOrEmpty(cleaned) ? null : cleaned;
    }
}
