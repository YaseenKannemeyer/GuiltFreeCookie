package za.ac.cput.guiltfreecookie.service.flow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Cart;
import za.ac.cput.guiltfreecookie.domain.CartStatus;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.DiscountCode;
import za.ac.cput.guiltfreecookie.domain.DiscountType;
import za.ac.cput.guiltfreecookie.domain.EmailFlowSettings;
import za.ac.cput.guiltfreecookie.domain.FlowEnrollment;
import za.ac.cput.guiltfreecookie.domain.OrderItem;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.ShippingSettings;
import za.ac.cput.guiltfreecookie.domain.StockStatus;
import za.ac.cput.guiltfreecookie.dto.cart.CartLine;
import za.ac.cput.guiltfreecookie.dto.catalog.CatalogQuery;
import za.ac.cput.guiltfreecookie.dto.catalog.CookieSummary;
import za.ac.cput.guiltfreecookie.dto.discount.DiscountRequest;
import za.ac.cput.guiltfreecookie.dto.engagement.ReviewView;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.repository.CartItemRepository;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.DiscountCodeRepository;
import za.ac.cput.guiltfreecookie.repository.NewsletterSubscriberRepository;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;
import za.ac.cput.guiltfreecookie.service.cart.ShopCartService;
import za.ac.cput.guiltfreecookie.service.catalog.CatalogService;
import za.ac.cput.guiltfreecookie.service.discount.DiscountService;
import za.ac.cput.guiltfreecookie.service.engagement.ReviewService;
import za.ac.cput.guiltfreecookie.service.order.OrderLinks;
import za.ac.cput.guiltfreecookie.service.shipping.ShippingSettingsService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// Decides whether a flow step still makes sense, and gathers what its email shows.
@Service
public class FlowDataLoader {

    public record Result(FlowData data, String exitReason, boolean skip, String issuedCode) {

        static Result exit(String reason) {
            return new Result(null, reason, false, null);
        }

        static Result skipStep() {
            return new Result(null, null, true, null);
        }
    }

    static final int CART_CODE_DAYS = 3;
    static final int WINBACK_CODE_DAYS = 7;
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter EXPIRY = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CatalogService catalogService;
    private final ReviewService reviewService;
    private final ShippingSettingsService shippingSettingsService;
    private final ShopCartService shopCartService;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CookieRepository cookieRepository;
    private final DiscountService discountService;
    private final DiscountCodeRepository discountCodeRepository;
    private final NewsletterSubscriberRepository subscriberRepository;
    private final OrderLinks orderLinks;
    private final MarketingLinks links;
    private final String publicBase;

    public FlowDataLoader(CatalogService catalogService, ReviewService reviewService,
                          ShippingSettingsService shippingSettingsService, ShopCartService shopCartService,
                          CartRepository cartRepository, CartItemRepository cartItemRepository,
                          CustomerOrderRepository orderRepository, OrderItemRepository orderItemRepository,
                          CookieRepository cookieRepository, DiscountService discountService,
                          DiscountCodeRepository discountCodeRepository,
                          NewsletterSubscriberRepository subscriberRepository, OrderLinks orderLinks,
                          MarketingLinks links, @Value("${app.public-base-url}") String publicBase) {
        this.catalogService = catalogService;
        this.reviewService = reviewService;
        this.shippingSettingsService = shippingSettingsService;
        this.shopCartService = shopCartService;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cookieRepository = cookieRepository;
        this.discountService = discountService;
        this.discountCodeRepository = discountCodeRepository;
        this.subscriberRepository = subscriberRepository;
        this.orderLinks = orderLinks;
        this.links = links;
        this.publicBase = publicBase.replaceAll("/+$", "");
    }

    @Transactional
    public Result load(FlowEnrollment enrollment, int step, EmailFlowSettings settings) {
        return switch (enrollment.getFlow()) {
            case WELCOME -> welcome(enrollment, step, settings);
            case BROWSE_ABANDONMENT -> browse(enrollment, step);
            case CART_ABANDONMENT -> cart(enrollment, step, settings);
            case POST_PURCHASE -> postPurchase(enrollment);
            case REPLENISHMENT -> replenishment(enrollment, settings);
            case WINBACK -> winback(enrollment, step, settings);
            case SUNSET -> sunset(enrollment, step);
        };
    }

    private Result welcome(FlowEnrollment enrollment, int step, EmailFlowSettings settings) {
        if (step > 0 && purchasedSince(enrollment)) {
            return Result.exit("Purchased");
        }
        int percent = settings.getWelcomeDiscountPercent();
        return switch (step) {
            case 0 -> percent > 0
                    ? withCode(issue(enrollment.getEmail(), "WELCOME", percent, settings.getWelcomeCodeDays(), "Welcome code"), percent)
                    : send(data().build());
            case 4 -> usableCode(enrollment.getDiscountCode())
                    .map(code -> send(data().code(code, percent).build()))
                    .orElseGet(Result::skipStep);
            default -> send(data().products(favourites()).reviews(reviews()).facts(deliveryFacts()).build());
        };
    }

    private Result browse(FlowEnrollment enrollment, int step) {
        if (purchasedSince(enrollment)) {
            return Result.exit("Purchased");
        }
        if (cartItemRepository.addedByEmailSince(enrollment.getEmail(), enrollment.getEnrolledAt())) {
            return Result.exit("Added to cart");
        }
        Cookie cookie = cookieRepository.findById(enrollment.getContextRef())
                .filter(found -> !found.isArchived() && found.getStockQuantity() > 0).orElse(null);
        if (cookie == null) {
            return Result.exit("Cookie unavailable");
        }
        List<Map<String, Object>> products = step == 0
                ? products(catalogService.summaries(List.of(cookie)))
                : related(cookie);
        return send(data().products(products).reviews(reviews()).cookieName(cookie.getDescription())
                .primary(links.frontend("/product/" + cookie.getSlug())).build());
    }

    private Result cart(FlowEnrollment enrollment, int step, EmailFlowSettings settings) {
        Cart cart = cartRepository.findById(enrollment.getContextRef())
                .filter(found -> found.getStatus() == CartStatus.ACTIVE).orElse(null);
        if (cart == null) {
            return Result.exit("Cart checked out or cleared");
        }
        if (purchasedSince(enrollment)) {
            return Result.exit("Purchased");
        }
        List<CartLine> lines = shopCartService.get(cart.getCartId()).items();
        if (lines.isEmpty()) {
            return Result.exit("Cart emptied");
        }
        Builder data = data()
                .items(lines.stream().map(line -> item(line.cookieId(), line.name(), line.variantLabel(),
                        line.quantity(), line.lineTotal())).toList())
                .facts(deliveryFacts())
                .primary(cart.isAccountOwned() || cart.getRecoveryToken() == null
                        ? links.frontend("/login?redirect=/cart")
                        : links.frontend("/cart/recover/" + cart.getRecoveryToken()));
        int percent = settings.getCartDiscountPercent();
        if (step == 2 && percent > 0) {
            DiscountCode code = issue(enrollment.getEmail(), "CART", percent, CART_CODE_DAYS, "Cart reminder code");
            return new Result(data.code(code, percent).build(), null, false, code.getCode());
        }
        return send(data.build());
    }

    private Result postPurchase(FlowEnrollment enrollment) {
        CustomerOrder order = liveOrder(enrollment);
        if (order == null) {
            return Result.exit("Order refunded or cancelled");
        }
        return send(data().primary(links.frontend("/review/" + order.getOrderNumber() + "?token=" + orderLinks.token(order)))
                .build());
    }

    private Result replenishment(FlowEnrollment enrollment, EmailFlowSettings settings) {
        CustomerOrder order = liveOrder(enrollment);
        if (order == null) {
            return Result.exit("Order refunded or cancelled");
        }
        if (purchasedSince(enrollment)) {
            return Result.exit("Ordered again");
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderItem item : orderItemRepository.findByOrderId(order.getOrderId())) {
            boolean available = cookieRepository.findById(item.getCookieId())
                    .filter(cookie -> !cookie.isArchived() && cookie.getStockQuantity() > 0).isPresent();
            if (available) {
                items.add(item(item.getCookieId(), item.getCookieName(), item.getVariantLabel(), item.getQuantity(), item.getLineTotal()));
            }
        }
        if (items.isEmpty()) {
            return Result.exit("Nothing from the order is available");
        }
        return send(data().items(items).replenishmentDays(settings.getReplenishmentDays())
                .primary(links.frontend("/reorder/" + order.getOrderNumber() + "?token=" + orderLinks.token(order)))
                .build());
    }

    private Result winback(FlowEnrollment enrollment, int step, EmailFlowSettings settings) {
        if (purchasedSince(enrollment)) {
            return Result.exit("Purchased");
        }
        int percent = settings.getWinbackDiscountPercent();
        return switch (step) {
            case 0 -> send(data().products(newest()).build());
            case 1 -> send(data().products(favourites()).reviews(reviews()).build());
            default -> percent > 0
                    ? withCode(issue(enrollment.getEmail(), "MISSYOU", percent, WINBACK_CODE_DAYS, "Winback code"), percent)
                    : send(data().build());
        };
    }

    private Result sunset(FlowEnrollment enrollment, int step) {
        if (engagedSince(enrollment)) {
            return Result.exit("Engaged");
        }
        String email = enrollment.getEmail();
        return send(data().products(step == 1 ? favourites() : List.of())
                .primary(links.frontend("/email/subscribed?e=" + MarketingLinks.encode(email) + "&t=" + links.token(email)))
                .build());
    }

    public boolean engagedSince(FlowEnrollment enrollment) {
        return subscriberRepository.findById(enrollment.getEmail())
                .map(subscriber -> subscriber.getLastEngagedAt() != null
                        && subscriber.getLastEngagedAt().isAfter(enrollment.getEnrolledAt()
                        .atZone(ZoneId.systemDefault()).toInstant()))
                .orElse(false);
    }

    private boolean purchasedSince(FlowEnrollment enrollment) {
        return orderRepository.existsByCustomerEmailAndStatusInAndPaidAtAfter(
                enrollment.getEmail(), OrderStatus.PAID_STATUSES, enrollment.getEnrolledAt());
    }

    private CustomerOrder liveOrder(FlowEnrollment enrollment) {
        return orderRepository.findByOrderNumber(enrollment.getContextRef())
                .filter(order -> order.getStatus().isPaid()).orElse(null);
    }

    private Optional<DiscountCode> usableCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now();
        return discountCodeRepository.findByCode(code)
                .filter(DiscountCode::isActive)
                .filter(found -> found.getMaxUses() == null || found.getUsedCount() < found.getMaxUses())
                .filter(found -> found.getEndsAt() == null || found.getEndsAt().isAfter(now));
    }

    // A single-use code that only works for this address.
    private DiscountCode issue(String email, String prefix, int percent, int days, String description) {
        LocalDateTime now = LocalDateTime.now();
        for (int attempt = 0; ; attempt++) {
            StringBuilder code = new StringBuilder(prefix).append('-');
            for (int i = 0; i < 6; i++) {
                code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            try {
                return discountService.create(new DiscountRequest(code.toString(), DiscountType.PERCENT,
                        BigDecimal.valueOf(percent), null, now, now.plusDays(days), 1, true, email, description, true));
            } catch (ConflictException e) {
                if (attempt >= 4) {
                    throw e;
                }
            }
        }
    }

    private Result withCode(DiscountCode code, int percent) {
        return new Result(data().code(code, percent).build(), null, false, code.getCode());
    }

    private static Result send(FlowData data) {
        return new Result(data, null, false, null);
    }

    private List<Map<String, Object>> favourites() {
        List<Map<String, Object>> featured = products(catalogService.search(
                new CatalogQuery(null, null, null, null, true, true, null, 0, 4)).items());
        return featured.isEmpty() ? newest() : featured;
    }

    private List<Map<String, Object>> newest() {
        return products(catalogService.search(
                new CatalogQuery(null, null, null, null, true, null, "newest", 0, 4)).items());
    }

    private List<Map<String, Object>> related(Cookie cookie) {
        List<Map<String, Object>> related = products(catalogService.summaries(
                cookieRepository.findTop8ByCategoryAndArchivedFalseAndCookieIdNot(cookie.getCategory(), cookie.getCookieId())));
        return related.isEmpty() ? favourites() : related.stream().limit(4).toList();
    }

    private List<Map<String, Object>> products(List<CookieSummary> summaries) {
        return summaries.stream()
                .filter(summary -> summary.stockStatus() != StockStatus.OUT_OF_STOCK)
                .<Map<String, Object>>map(summary -> {
                    Map<String, Object> product = new HashMap<>();
                    product.put("name", summary.name());
                    product.put("price", summary.fromPrice() == null ? "" : rand(summary.fromPrice()));
                    product.put("imageUrl", image(summary.cookieId()));
                    product.put("url", links.frontend("/product/" + summary.slug()));
                    return product;
                })
                .toList();
    }

    private List<Map<String, Object>> reviews() {
        return reviewService.featured(2).stream().map(FlowDataLoader::review).toList();
    }

    private static Map<String, Object> review(ReviewView review) {
        return Map.of(
                "quote", review.body(),
                "author", review.authorName(),
                "stars", "★".repeat(review.rating()) + "☆".repeat(5 - review.rating()),
                "rating", review.rating());
    }

    private Map<String, Object> item(String cookieId, String name, String variant, int quantity, BigDecimal lineTotal) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("variant", variant);
        item.put("quantity", quantity);
        item.put("lineTotal", rand(lineTotal));
        item.put("imageUrl", image(cookieId));
        return item;
    }

    private List<String> deliveryFacts() {
        ShippingSettings shipping = shippingSettingsService.get();
        List<String> facts = new ArrayList<>();
        if (shipping.isCourierEnabled()) {
            facts.add("Courier to your door: " + rand(shipping.getCourierRate()) + " (" + shipping.getCourierEta() + ").");
        }
        if (shipping.isPudoEnabled()) {
            facts.add("PUDO locker near you: " + rand(shipping.getPudoRate()) + ".");
        }
        if (shipping.isPickupEnabled()) {
            facts.add("Collect from our bakery in Strandfontein for free.");
        }
        if (shipping.getFreeShippingThreshold() != null && (shipping.isCourierEnabled() || shipping.isPudoEnabled())) {
            facts.add("Free delivery on orders of " + rand(shipping.getFreeShippingThreshold()) + " or more.");
        }
        return facts;
    }

    private String image(String cookieId) {
        return publicBase + "/api/v1/email-assets/cookies/" + cookieId + ".png";
    }

    static String rand(BigDecimal amount) {
        return "R " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static Builder data() {
        return new Builder();
    }

    private static final class Builder {
        private List<Map<String, Object>> products = List.of();
        private List<Map<String, Object>> items = List.of();
        private List<Map<String, Object>> reviews = List.of();
        private List<String> facts = List.of();
        private String code;
        private String expiry;
        private int percent;
        private String cookieName;
        private String primary;
        private int replenishmentDays;

        private Builder products(List<Map<String, Object>> products) {
            this.products = products;
            return this;
        }

        private Builder items(List<Map<String, Object>> items) {
            this.items = items;
            return this;
        }

        private Builder reviews(List<Map<String, Object>> reviews) {
            this.reviews = reviews;
            return this;
        }

        private Builder facts(List<String> facts) {
            this.facts = facts;
            return this;
        }

        private Builder code(DiscountCode discount, int percent) {
            this.code = discount.getCode();
            this.expiry = discount.getEndsAt() == null ? null : discount.getEndsAt().format(EXPIRY);
            this.percent = percent;
            return this;
        }

        private Builder cookieName(String cookieName) {
            this.cookieName = cookieName;
            return this;
        }

        private Builder primary(String primary) {
            this.primary = primary;
            return this;
        }

        private Builder replenishmentDays(int replenishmentDays) {
            this.replenishmentDays = replenishmentDays;
            return this;
        }

        private FlowData build() {
            return new FlowData(products, items, reviews, facts, code, expiry, percent, cookieName, primary, replenishmentDays);
        }
    }
}
