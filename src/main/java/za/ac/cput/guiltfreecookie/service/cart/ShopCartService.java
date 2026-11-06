package za.ac.cput.guiltfreecookie.service.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Cart;
import za.ac.cput.guiltfreecookie.domain.CartItem;
import za.ac.cput.guiltfreecookie.domain.CartStatus;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.dto.cart.AddItemRequest;
import za.ac.cput.guiltfreecookie.dto.cart.CartView;
import za.ac.cput.guiltfreecookie.dto.shipping.ShippingQuote;
import za.ac.cput.guiltfreecookie.service.shipping.DeliveryArea;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CartItemRepository;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CookieVariantRepository;
import za.ac.cput.guiltfreecookie.service.discount.DiscountService;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// The storefront cart. The cart id is the guest's key (a random UUID kept in the browser).
@Service
public class ShopCartService {

    static final int MAX_LINES = 30;

    private final CartRepository cartRepository;
    private final CartItemRepository itemRepository;
    private final CookieRepository cookieRepository;
    private final CookieVariantRepository variantRepository;
    private final PricingService pricingService;
    private final DiscountService discountService;

    public ShopCartService(CartRepository cartRepository, CartItemRepository itemRepository,
                           CookieRepository cookieRepository, CookieVariantRepository variantRepository,
                           PricingService pricingService, DiscountService discountService) {
        this.cartRepository = cartRepository;
        this.itemRepository = itemRepository;
        this.cookieRepository = cookieRepository;
        this.variantRepository = variantRepository;
        this.pricingService = pricingService;
        this.discountService = discountService;
    }

    // A signed-in customer gets their existing cart back instead of a second one.
    @Transactional
    public CartView create(String customerEmail) {
        if (customerEmail != null) {
            Cart existing = activeAccountCart(customerEmail);
            if (existing != null) {
                return view(existing);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        Cart cart = cartRepository.save(new Cart.Builder()
                .setCartId(UUID.randomUUID().toString())
                .setCustomerEmail(customerEmail)
                .setAccountOwned(customerEmail != null)
                .setStatus(CartStatus.ACTIVE)
                .setTotalPrice(0)
                .setCreatedAt(now)
                .setUpdatedAt(now)
                .build());
        return view(cart);
    }

    @Transactional(readOnly = true)
    public CartView get(String cartId) {
        return get(cartId, null);
    }

    public CartView get(String cartId, ShippingMethod shippingMethod) {
        return get(cartId, shippingMethod, null);
    }

    // With a shipping method the view includes delivery in the total, exactly as checkout will charge it.
    @Transactional
    public CartView get(String cartId, ShippingMethod shippingMethod, DeliveryArea area) {
        Cart cart = cartRepository.findById(cartId).orElseThrow(ShopCartService::cartNotFound);
        return pricingService.price(cart, itemRepository.findByCartIdOrderByAddedAtAsc(cartId), shippingMethod, area);
    }

    @Transactional
    public List<ShippingQuote> shippingOptions(String cartId, DeliveryArea area) {
        return pricingService.shippingOptions(get(cartId, null), area);
    }

    @Transactional(readOnly = true)
    public CartView myCart(String customerEmail) {
        Cart cart = activeAccountCart(customerEmail);
        if (cart == null) {
            throw new NotFoundException("You don't have a cart yet");
        }
        return view(cart);
    }

    @Transactional
    public CartView addItem(String cartId, AddItemRequest request) {
        Cart cart = lockActive(cartId);
        int quantity = validQuantity(request.quantity());
        CookieVariant variant = request.variantId() == null ? null
                : variantRepository.findById(request.variantId()).filter(CookieVariant::isActive).orElse(null);
        Cookie cookie = variant == null ? null
                : cookieRepository.findById(variant.getCookieId()).filter(found -> !found.isArchived()).orElse(null);
        if (cookie == null) {
            throw new NotFoundException("That cookie or pack size isn't available");
        }

        List<CartItem> items = itemRepository.findByCartIdOrderByAddedAtAsc(cartId);
        CartItem existing = items.stream().filter(item -> item.getVariantId().equals(variant.getVariantId())).findFirst().orElse(null);
        if (existing == null && items.size() >= MAX_LINES) {
            throw new BadRequestException("A cart can hold up to " + MAX_LINES + " different items");
        }
        int newQuantity = (existing == null ? 0 : existing.getQuantity()) + quantity;
        if (newQuantity > PricingService.MAX_QUANTITY) {
            throw new BadRequestException("You can order up to " + PricingService.MAX_QUANTITY + " of each item");
        }
        requireStock(cookie, variant, newQuantity, items, existing);

        itemRepository.save(existing == null
                ? new CartItem.Builder().setCartId(cartId).setVariantId(variant.getVariantId())
                        .setCookieId(cookie.getCookieId()).setQuantity(newQuantity).setAddedAt(LocalDateTime.now()).build()
                : new CartItem.Builder().copy(existing).setQuantity(newQuantity).build());
        return touch(cart);
    }

    // Quantity 0 removes the line.
    @Transactional
    public CartView updateQuantity(String cartId, String cartItemId, Integer quantity) {
        Cart cart = lockActive(cartId);
        CartItem item = requireItem(cartId, cartItemId);
        if (quantity != null && quantity == 0) {
            itemRepository.delete(item);
            return touch(cart);
        }
        int newQuantity = validQuantity(quantity);
        if (newQuantity > item.getQuantity()) {
            CookieVariant variant = variantRepository.findById(item.getVariantId()).filter(CookieVariant::isActive)
                    .orElseThrow(() -> new ConflictException("This pack size is no longer available: remove it from your cart"));
            Cookie cookie = cookieRepository.findById(item.getCookieId()).filter(found -> !found.isArchived())
                    .orElseThrow(() -> new ConflictException("This cookie is no longer available: remove it from your cart"));
            requireStock(cookie, variant, newQuantity, itemRepository.findByCartIdOrderByAddedAtAsc(cartId), item);
        }
        itemRepository.save(new CartItem.Builder().copy(item).setQuantity(newQuantity).build());
        return touch(cart);
    }

    @Transactional
    public CartView removeItem(String cartId, String cartItemId) {
        Cart cart = lockActive(cartId);
        itemRepository.delete(requireItem(cartId, cartItemId));
        return touch(cart);
    }

    @Transactional
    public CartView clear(String cartId) {
        Cart cart = lockActive(cartId);
        itemRepository.deleteByCartId(cartId);
        return touch(new Cart.Builder().copy(cart).setDiscountCode(null).build());
    }

    @Transactional
    public CartView applyDiscount(String cartId, String code) {
        Cart cart = lockActive(cartId);
        BigDecimal subtotal = view(cart).subtotal();
        String applied = discountService.requireApplicable(code, subtotal, cart.getCustomerEmail()).getCode();
        return touch(new Cart.Builder().copy(cart).setDiscountCode(applied).build());
    }

    @Transactional
    public CartView removeDiscount(String cartId) {
        Cart cart = lockActive(cartId);
        return touch(new Cart.Builder().copy(cart).setDiscountCode(null).build());
    }

    // Lets a guest's cart be recovered by email later (abandoned-cart reminders).
    @Transactional
    public CartView setEmail(String cartId, String rawEmail) {
        Cart cart = lockActive(cartId);
        if (cart.isAccountOwned()) {
            throw new ConflictException("This cart uses your account's email address");
        }
        String email = Helper.cleanEmail(rawEmail);
        if (!Helper.isValidEmail(email)) {
            throw new BadRequestException("Enter a valid email address");
        }
        return touch(new Cart.Builder().copy(cart).setCustomerEmail(email).build());
    }

    // On login: the guest cart becomes the account cart, or its items move into the existing account cart.
    @Transactional
    public CartView merge(String customerEmail, String guestCartId) {
        Cart guest = guestCartId == null ? null : cartRepository.findForUpdate(guestCartId)
                .filter(cart -> cart.getStatus() == CartStatus.ACTIVE)
                .filter(cart -> !cart.isAccountOwned() || customerEmail.equals(cart.getCustomerEmail()))
                .orElse(null);
        if (guest == null) {
            throw cartNotFound();
        }
        Cart account = activeAccountCart(customerEmail);
        if (account == null || account.getCartId().equals(guest.getCartId())) {
            return touch(new Cart.Builder().copy(guest).setCustomerEmail(customerEmail).setAccountOwned(true).build());
        }

        account = cartRepository.findForUpdate(account.getCartId()).orElseThrow();
        List<CartItem> accountItems = itemRepository.findByCartIdOrderByAddedAtAsc(account.getCartId());
        for (CartItem guestItem : itemRepository.findByCartIdOrderByAddedAtAsc(guest.getCartId())) {
            moveInto(account, accountItems, guestItem);
            accountItems = itemRepository.findByCartIdOrderByAddedAtAsc(account.getCartId());
        }
        itemRepository.deleteByCartId(guest.getCartId());
        touch(new Cart.Builder().copy(guest).setStatus(CartStatus.CLEARED).setDiscountCode(null).build());
        String code = account.getDiscountCode() != null ? account.getDiscountCode() : guest.getDiscountCode();
        return touch(new Cart.Builder().copy(account).setDiscountCode(code).build());
    }

    // Adds what still fits (stock, per-line limit); unavailable items are dropped.
    private void moveInto(Cart account, List<CartItem> accountItems, CartItem guestItem) {
        CookieVariant variant = variantRepository.findById(guestItem.getVariantId()).filter(CookieVariant::isActive).orElse(null);
        Cookie cookie = variant == null ? null
                : cookieRepository.findById(variant.getCookieId()).filter(found -> !found.isArchived()).orElse(null);
        if (cookie == null) {
            return;
        }
        CartItem existing = accountItems.stream()
                .filter(item -> item.getVariantId().equals(variant.getVariantId())).findFirst().orElse(null);
        if (existing == null && accountItems.size() >= MAX_LINES) {
            return;
        }
        int unitsElsewhere = unitsOfCookie(accountItems, cookie.getCookieId(), existing);
        int fits = Math.max(0, (cookie.getStockQuantity() - unitsElsewhere) / variant.getUnitsPerPack());
        int quantity = Math.min(Math.min((existing == null ? 0 : existing.getQuantity()) + guestItem.getQuantity(),
                PricingService.MAX_QUANTITY), fits);
        if (quantity <= 0) {
            return;
        }
        itemRepository.save(existing == null
                ? new CartItem.Builder().setCartId(account.getCartId()).setVariantId(variant.getVariantId())
                        .setCookieId(cookie.getCookieId()).setQuantity(quantity).setAddedAt(LocalDateTime.now()).build()
                : new CartItem.Builder().copy(existing).setQuantity(quantity).build());
    }

    private void requireStock(Cookie cookie, CookieVariant variant, int newQuantity, List<CartItem> items, CartItem replacing) {
        if (unitsOfCookie(items, cookie.getCookieId(), replacing) + newQuantity * variant.getUnitsPerPack() <= cookie.getStockQuantity()) {
            return;
        }
        int inCart = unitsOfCookie(items, cookie.getCookieId(), null);
        int left = Math.max(0, cookie.getStockQuantity() - inCart);
        String name = cookie.getDescription();
        if (cookie.getStockQuantity() <= 0) {
            throw new ConflictException("Sorry, " + name + " is sold out");
        }
        if (left == 0) {
            throw new ConflictException("You already have all the " + name + " cookies we have in stock");
        }
        throw new ConflictException(inCart == 0
                ? "Sorry, we only have " + left + " " + name + " cookies left"
                : "Sorry, you can only add " + left + " more " + name + " cookies");
    }

    private int unitsOfCookie(List<CartItem> items, String cookieId, CartItem excluding) {
        int units = 0;
        for (CartItem item : items) {
            if (item.getCookieId().equals(cookieId) && (excluding == null || !item.getCartItemId().equals(excluding.getCartItemId()))) {
                units += item.getQuantity() * variantRepository.findById(item.getVariantId())
                        .map(CookieVariant::getUnitsPerPack).orElse(0);
            }
        }
        return units;
    }

    private Cart activeAccountCart(String customerEmail) {
        return cartRepository.findFirstByCustomerEmailAndAccountOwnedTrueAndStatusOrderByUpdatedAtDesc(
                customerEmail, CartStatus.ACTIVE).orElse(null);
    }

    private Cart lockActive(String cartId) {
        Cart cart = cartRepository.findForUpdate(cartId).orElseThrow(ShopCartService::cartNotFound);
        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new ConflictException("This cart has already been checked out or cleared");
        }
        return cart;
    }

    private CartItem requireItem(String cartId, String cartItemId) {
        return itemRepository.findById(cartItemId)
                .filter(item -> item.getCartId().equals(cartId))
                .orElseThrow(() -> new NotFoundException("That item isn't in this cart"));
    }

    // Saves the cart with its freshly priced total, so the admin /cart views stay accurate.
    private CartView touch(Cart cart) {
        CartView priced = pricingService.price(cart, itemRepository.findByCartIdOrderByAddedAtAsc(cart.getCartId()));
        cartRepository.save(new Cart.Builder().copy(cart)
                .setTotalPrice(priced.total().doubleValue())
                .setUpdatedAt(LocalDateTime.now())
                .build());
        return priced;
    }

    private CartView view(Cart cart) {
        return pricingService.price(cart, itemRepository.findByCartIdOrderByAddedAtAsc(cart.getCartId()));
    }

    private static int validQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > PricingService.MAX_QUANTITY) {
            throw new BadRequestException("Quantity must be between 1 and " + PricingService.MAX_QUANTITY);
        }
        return quantity;
    }

    private static NotFoundException cartNotFound() {
        return new NotFoundException("Cart not found");
    }
}
