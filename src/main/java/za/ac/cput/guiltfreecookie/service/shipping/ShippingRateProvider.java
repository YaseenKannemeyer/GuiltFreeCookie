package za.ac.cput.guiltfreecookie.service.shipping;

import za.ac.cput.guiltfreecookie.dto.shipping.ShippingQuote;

import java.math.BigDecimal;
import java.util.List;

// How delivery is priced. RuleBasedShippingProvider uses the admin's flat rates; a live
// Courier Guy or PUDO integration would implement this and replace it.
public interface ShippingRateProvider {

    // merchandiseTotal is the cart subtotal after discounts.
    List<ShippingQuote> quotes(BigDecimal merchandiseTotal, boolean freeShippingCode);
}
