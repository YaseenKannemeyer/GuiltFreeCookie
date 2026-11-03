package za.ac.cput.guiltfreecookie.service.shipping;

import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.domain.ShippingSettings;
import za.ac.cput.guiltfreecookie.dto.shipping.ShippingQuote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class RuleBasedShippingProvider implements ShippingRateProvider {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final ShippingSettingsService settingsService;

    public RuleBasedShippingProvider(ShippingSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public List<ShippingQuote> quotes(BigDecimal merchandiseTotal, boolean freeShippingCode) {
        ShippingSettings settings = settingsService.get();
        boolean overThreshold = settings.getFreeShippingThreshold() != null
                && merchandiseTotal.compareTo(settings.getFreeShippingThreshold()) >= 0;
        boolean free = overThreshold || freeShippingCode;

        List<ShippingQuote> quotes = new ArrayList<>();
        if (settings.isCourierEnabled()) {
            quotes.add(new ShippingQuote(ShippingMethod.COURIER, "Courier to your door",
                    free ? ZERO : money(settings.getCourierRate()), settings.getCourierEta(), free, true, null));
        }
        if (settings.isPudoEnabled()) {
            quotes.add(new ShippingQuote(ShippingMethod.PUDO, "PUDO locker",
                    free ? ZERO : money(settings.getPudoRate()), settings.getPudoEta(), free, false, null));
        }
        if (settings.isPickupEnabled()) {
            String details = settings.getPickupInstructions() == null
                    ? settings.getPickupAddress()
                    : settings.getPickupAddress() + "\n" + settings.getPickupInstructions();
            quotes.add(new ShippingQuote(ShippingMethod.PICKUP, "Collect from the bakery", ZERO,
                    settings.getPickupEta(), false, false, details));
        }
        return quotes;
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
