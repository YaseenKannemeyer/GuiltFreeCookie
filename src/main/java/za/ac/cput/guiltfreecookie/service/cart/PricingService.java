package za.ac.cput.guiltfreecookie.service.cart;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.domain.Cart;
import za.ac.cput.guiltfreecookie.domain.CartItem;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.dto.cart.CartLine;
import za.ac.cput.guiltfreecookie.dto.cart.CartView;
import za.ac.cput.guiltfreecookie.dto.cart.DiscountView;
import za.ac.cput.guiltfreecookie.dto.cart.LineIssue;
import za.ac.cput.guiltfreecookie.dto.shipping.ShippingQuote;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CookieVariantRepository;
import za.ac.cput.guiltfreecookie.service.discount.DiscountService;
import za.ac.cput.guiltfreecookie.service.shipping.ShippingRateProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// The one place cart totals are worked out. Prices always come from the database, never the client.
@Service
public class PricingService {

    public static final int MAX_QUANTITY = 50;
    static final String CURRENCY = "ZAR";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final CookieRepository cookieRepository;
    private final CookieVariantRepository variantRepository;
    private final DiscountService discountService;
    private final ShippingRateProvider shippingRateProvider;
    private final boolean vatRegistered;
    private final BigDecimal vatRate;

    public PricingService(CookieRepository cookieRepository, CookieVariantRepository variantRepository,
                          DiscountService discountService, ShippingRateProvider shippingRateProvider,
                          @Value("${app.vat.registered}") boolean vatRegistered,
                          @Value("${app.vat.rate}") BigDecimal vatRate) {
        this.cookieRepository = cookieRepository;
        this.variantRepository = variantRepository;
        this.discountService = discountService;
        this.shippingRateProvider = shippingRateProvider;
        this.vatRegistered = vatRegistered;
        this.vatRate = vatRate;
    }

    public CartView price(Cart cart, List<CartItem> items) {
        return price(cart, items, null);
    }

    // With a shippingMethod, shipping is priced and included in the total.
    public CartView price(Cart cart, List<CartItem> items, ShippingMethod shippingMethod) {
        Map<String, CookieVariant> variants = variantRepository
                .findAllById(items.stream().map(CartItem::getVariantId).distinct().toList())
                .stream().collect(Collectors.toMap(CookieVariant::getVariantId, Function.identity()));
        Map<String, Cookie> cookies = cookieRepository
                .findAllById(items.stream().map(CartItem::getCookieId).distinct().toList())
                .stream().collect(Collectors.toMap(Cookie::getCookieId, Function.identity()));

        Map<String, Integer> unitsByCookie = new HashMap<>();
        for (CartItem item : items) {
            if (isPurchasable(variants.get(item.getVariantId()), cookies.get(item.getCookieId()))) {
                unitsByCookie.merge(item.getCookieId(), units(item, variants), Integer::sum);
            }
        }

        List<CartLine> lines = new ArrayList<>();
        BigDecimal subtotal = ZERO;
        int itemCount = 0;
        int cookieCount = 0;
        for (CartItem item : items) {
            CookieVariant variant = variants.get(item.getVariantId());
            Cookie cookie = cookies.get(item.getCookieId());
            if (!isPurchasable(variant, cookie)) {
                lines.add(unavailableLine(item, variant, cookie));
                continue;
            }
            int unitsElsewhere = unitsByCookie.get(item.getCookieId()) - units(item, variants);
            int maxQuantity = Math.clamp((cookie.getStockQuantity() - unitsElsewhere) / variant.getUnitsPerPack(), 0, MAX_QUANTITY);
            BigDecimal unitPrice = variant.effectivePrice(cookie);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            lines.add(new CartLine(item.getCartItemId(), cookie.getCookieId(), cookie.getSlug(), cookie.getDescription(),
                    cookie.getImage(), cookie.getAllergies(), variant.getVariantId(), variant.getLabel(), variant.getUnitsPerPack(), unitPrice,
                    item.getQuantity(), lineTotal, maxQuantity,
                    item.getQuantity() > maxQuantity ? LineIssue.NOT_ENOUGH_STOCK : null));
            subtotal = subtotal.add(lineTotal);
            itemCount += item.getQuantity();
            cookieCount += units(item, variants);
        }

        DiscountView discount = null;
        BigDecimal discountTotal = ZERO;
        if (cart.getDiscountCode() != null) {
            DiscountService.Evaluation evaluation =
                    discountService.evaluate(cart.getDiscountCode(), subtotal, cart.getCustomerEmail());
            discount = new DiscountView(
                    cart.getDiscountCode(),
                    evaluation.discount() == null ? null : evaluation.discount().getType(),
                    evaluation.discount() == null ? null : evaluation.discount().getDescription(),
                    evaluation.amount(),
                    evaluation.freeShipping(),
                    evaluation.applied(),
                    evaluation.message());
            discountTotal = evaluation.amount();
        }

        BigDecimal merchandiseTotal = subtotal.subtract(discountTotal).max(ZERO);
        BigDecimal shipping = null;
        if (shippingMethod != null) {
            shipping = quote(merchandiseTotal, discount != null && discount.freeShipping(), shippingMethod).cost();
        }
        BigDecimal total = shipping == null ? merchandiseTotal : merchandiseTotal.add(shipping);
        boolean canCheckout = !lines.isEmpty() && lines.stream().allMatch(line -> line.issue() == null);
        return new CartView(cart.getCartId(), cart.getStatus(), cart.getCustomerEmail(), cart.isAccountOwned(), lines,
                itemCount, cookieCount, subtotal, discount, discountTotal, shippingMethod, shipping, vatIncluded(total),
                total, CURRENCY, canCheckout);
    }

    public List<ShippingQuote> shippingOptions(CartView cart) {
        BigDecimal merchandiseTotal = cart.subtotal().subtract(cart.discountTotal()).max(ZERO);
        return shippingRateProvider.quotes(merchandiseTotal, cart.discount() != null && cart.discount().freeShipping());
    }

    private ShippingQuote quote(BigDecimal merchandiseTotal, boolean freeShippingCode, ShippingMethod method) {
        return shippingRateProvider.quotes(merchandiseTotal, freeShippingCode).stream()
                .filter(quote -> quote.method() == method)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("That delivery option isn't available right now"));
    }

    // Prices are VAT-inclusive; this is the VAT portion to show on the receipt (null when not VAT-registered).
    BigDecimal vatIncluded(BigDecimal total) {
        if (!vatRegistered) {
            return null;
        }
        return total.multiply(vatRate).divide(BigDecimal.ONE.add(vatRate), 2, RoundingMode.HALF_UP);
    }

    private static boolean isPurchasable(CookieVariant variant, Cookie cookie) {
        return variant != null && variant.isActive() && cookie != null && !cookie.isArchived();
    }

    private static int units(CartItem item, Map<String, CookieVariant> variants) {
        return item.getQuantity() * variants.get(item.getVariantId()).getUnitsPerPack();
    }

    private static CartLine unavailableLine(CartItem item, CookieVariant variant, Cookie cookie) {
        return new CartLine(item.getCartItemId(), item.getCookieId(),
                cookie == null ? null : cookie.getSlug(),
                cookie == null ? "No longer available" : cookie.getDescription(),
                cookie == null ? null : cookie.getImage(),
                cookie == null ? null : cookie.getAllergies(),
                item.getVariantId(),
                variant == null ? null : variant.getLabel(),
                variant == null ? 0 : variant.getUnitsPerPack(),
                variant == null || cookie == null ? ZERO : variant.effectivePrice(cookie),
                item.getQuantity(), ZERO, 0, LineIssue.UNAVAILABLE);
    }
}
