package za.ac.cput.guiltfreecookie.service.shipping;

import za.ac.cput.guiltfreecookie.util.ShopMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.dto.shipping.ShippingQuote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// The admin's flat rates, with the courier price replaced by ShipLogic's once the delivery area is known.
// Any ShipLogic failure keeps the flat rate, so checkout never waits on the courier for long.
@Primary
@Service
public class LiveShippingProvider implements ShippingRateProvider {

    static final Duration CACHE_FOR = Duration.ofMinutes(10);
    private static final int SWEEP_THRESHOLD = 1_000;

    private record Cached(BigDecimal rate, Instant expires) {
    }

    private final RuleBasedShippingProvider flatRates;
    private final ShipLogicClient shipLogic;
    private final Clock clock;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    @Autowired
    public LiveShippingProvider(RuleBasedShippingProvider flatRates, ShipLogicClient shipLogic) {
        this(flatRates, shipLogic, Clock.systemUTC());
    }

    LiveShippingProvider(RuleBasedShippingProvider flatRates, ShipLogicClient shipLogic, Clock clock) {
        this.flatRates = flatRates;
        this.shipLogic = shipLogic;
        this.clock = clock;
    }

    @Override
    public List<ShippingQuote> quotes(ShippingRequest request) {
        List<ShippingQuote> quotes = flatRates.quotes(request);
        if (!shipLogic.enabled()) {
            return quotes;
        }
        return quotes.stream().map(quote -> courier(quote, request)).toList();
    }

    private ShippingQuote courier(ShippingQuote quote, ShippingRequest request) {
        if (quote.method() != ShippingMethod.COURIER || quote.freeShipping()) {
            return quote;
        }
        if (request.area() == null) {
            return quote.withCost(quote.cost(), true);
        }
        return liveRate(request).map(rate -> quote.withCost(rate, false)).orElse(quote);
    }

    private Optional<BigDecimal> liveRate(ShippingRequest request) {
        Instant now = clock.instant();
        String key = request.area().cacheKey() + "|" + request.cookieCount();
        Cached cached = cache.get(key);
        if (cached != null && now.isBefore(cached.expires())) {
            return Optional.of(cached.rate());
        }
        Optional<BigDecimal> rate = shipLogic.cheapestRate(request.area(), request.cookieCount(), request.merchandiseTotal())
                .map(value -> value.setScale(2, RoundingMode.HALF_UP));
        ShopMetrics.courierQuote(rate.isPresent() ? "live" : "fallback");
        rate.ifPresent(value -> {
            if (cache.size() > SWEEP_THRESHOLD) {
                cache.values().removeIf(entry -> !now.isBefore(entry.expires()));
            }
            cache.put(key, new Cached(value, now.plus(CACHE_FOR)));
        });
        return rate;
    }
}
