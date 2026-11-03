package za.ac.cput.guiltfreecookie.service.catalog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.domain.StockStatus;

import java.util.List;

@Component
public class StockRules {

    private final int defaultLowStockThreshold;

    public StockRules(@Value("${app.catalog.default-low-stock-threshold}") int defaultLowStockThreshold) {
        this.defaultLowStockThreshold = defaultLowStockThreshold;
    }

    // Out of stock when not even the smallest active pack can be filled.
    public StockStatus status(Cookie cookie, List<CookieVariant> activeVariants) {
        int stock = cookie.getStockQuantity();
        int smallestPack = activeVariants.stream().mapToInt(CookieVariant::getUnitsPerPack).min().orElse(Integer.MAX_VALUE);
        if (activeVariants.isEmpty() || stock < smallestPack) {
            return StockStatus.OUT_OF_STOCK;
        }
        return stock <= threshold(cookie) ? StockStatus.LOW_STOCK : StockStatus.IN_STOCK;
    }

    public int threshold(Cookie cookie) {
        return cookie.getLowStockThreshold() != null ? cookie.getLowStockThreshold() : defaultLowStockThreshold;
    }
}
