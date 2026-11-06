package za.ac.cput.guiltfreecookie.service.shipping;

import za.ac.cput.guiltfreecookie.dto.shipping.ShippingQuote;

import java.util.List;

// How delivery is priced. RuleBasedShippingProvider uses the admin's flat rates;
// LiveShippingProvider swaps in a live courier rate once the delivery area is known.
public interface ShippingRateProvider {

    List<ShippingQuote> quotes(ShippingRequest request);
}
