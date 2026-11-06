package za.ac.cput.guiltfreecookie.service.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Address;
import za.ac.cput.guiltfreecookie.domain.Cart;
import za.ac.cput.guiltfreecookie.domain.CartStatus;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.DiscountCode;
import za.ac.cput.guiltfreecookie.domain.OrderItem;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.dto.cart.CartLine;
import za.ac.cput.guiltfreecookie.dto.cart.CartView;
import za.ac.cput.guiltfreecookie.dto.order.AddressRequest;
import za.ac.cput.guiltfreecookie.dto.order.CheckoutRequest;
import za.ac.cput.guiltfreecookie.dto.order.CheckoutResponse;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CartItemRepository;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.DiscountCodeRepository;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;
import za.ac.cput.guiltfreecookie.service.NewsletterService;
import za.ac.cput.guiltfreecookie.service.cart.PricingService;
import za.ac.cput.guiltfreecookie.service.catalog.InventoryService;
import za.ac.cput.guiltfreecookie.service.payment.PaymentGateway;
import za.ac.cput.guiltfreecookie.service.shipping.DeliveryArea;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// Turns a cart into an order: re-prices on the server, holds the stock, then hands over to the payment gateway.
// Accounts are optional: a guest checks out with just an email address.
@Service
public class CheckoutService {

    static final int MAX_NAME_LENGTH = 100;
    static final int MAX_NOTE_LENGTH = 500;
    static final int MAX_LOCKER_LENGTH = 150;
    static final int MAX_ADDRESS_PART_LENGTH = 100;
    static final int MAX_STREET_NUMBER_LENGTH = 20;
    private static final String NUMBER_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DiscountCodeRepository discountRepository;
    private final PricingService pricingService;
    private final InventoryService inventoryService;
    private final NewsletterService newsletterService;
    private final PaymentGateway paymentGateway;
    private final OrderService orderService;
    private final OrderLinks orderLinks;
    private final long paymentWindowMinutes;
    static final int MAX_UNPAID_ORDERS = 3;

    // Includes unpaid orders, so the same email can't hold the code on two orders at once.
    static final Set<OrderStatus> ONE_PER_EMAIL_STATUSES = EnumSet.of(OrderStatus.PENDING_PAYMENT,
            OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.READY_FOR_PICKUP, OrderStatus.SHIPPED, OrderStatus.DELIVERED);
    private final SecureRandom random = new SecureRandom();

    public CheckoutService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                           CustomerOrderRepository orderRepository, OrderItemRepository orderItemRepository,
                           DiscountCodeRepository discountRepository, PricingService pricingService,
                           InventoryService inventoryService, NewsletterService newsletterService,
                           PaymentGateway paymentGateway, OrderService orderService, OrderLinks orderLinks,
                           @Value("${app.order.payment-window-minutes}") long paymentWindowMinutes) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.discountRepository = discountRepository;
        this.pricingService = pricingService;
        this.inventoryService = inventoryService;
        this.newsletterService = newsletterService;
        this.paymentGateway = paymentGateway;
        this.orderService = orderService;
        this.orderLinks = orderLinks;
        this.paymentWindowMinutes = paymentWindowMinutes;
    }

    // signedInEmail is the customer's account email, or null for a guest.
    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request, String signedInEmail) {
        Contact contact = validContact(request, signedInEmail);
        ShippingMethod method = request.shippingMethod();
        Address address = method == ShippingMethod.COURIER ? validAddress(request.address()) : null;
        String locker = method == ShippingMethod.PUDO ? validLocker(request.pudoLocker()) : null;
        String note = optionalText(request.note(), MAX_NOTE_LENGTH, "Your note");

        Cart cart = lockCheckoutCart(request.cartId(), signedInEmail);
        if (!cart.isAccountOwned()) {
            cart = cartRepository.save(new Cart.Builder().copy(cart).setCustomerEmail(contact.email()).build());
        }
        // The courier price is fixed here and stored on the order, whatever the courier charges later.
        CartView priced = pricingService.price(cart, cartItemRepository.findByCartIdOrderByAddedAtAsc(cart.getCartId()),
                method, DeliveryArea.of(address));
        requireCheckoutable(priced, contact.email());
        // Unpaid orders hold stock, so one address can't tie up the shelf with a pile of them.
        if (orderRepository.countByCustomerEmailAndStatus(contact.email(), OrderStatus.PENDING_PAYMENT) >= MAX_UNPAID_ORDERS) {
            throw new ConflictException("You already have unpaid orders waiting. Finish paying for one, "
                    + "or try again in " + paymentWindowMinutes + " minutes.");
        }
        // The use is taken now, not at payment, so a single-use code can't sit on several unpaid orders.
        if (priced.discount() != null && discountRepository.redeem(priced.discount().code()) == 0) {
            throw new ConflictException("Code " + priced.discount().code() + " has just been used up. Remove it to continue.");
        }

        String orderNumber = newOrderNumber();
        reserveStock(priced, orderNumber);
        LocalDateTime now = LocalDateTime.now();
        CustomerOrder order = orderRepository.save(new CustomerOrder.Builder()
                .setOrderNumber(orderNumber)
                .setCustomerEmail(contact.email())
                .setAccountOwned(signedInEmail != null)
                .setFirstName(contact.firstName())
                .setLastName(contact.lastName())
                .setPhone(contact.phone())
                .setShippingMethod(method)
                .setAddress(address)
                .setPudoLocker(locker)
                .setCustomerNote(note)
                .setCartId(cart.getCartId())
                .setSubtotal(priced.subtotal())
                .setDiscountCode(priced.discount() != null && priced.discount().applied() ? priced.discount().code() : null)
                .setDiscountTotal(priced.discountTotal())
                .setShippingTotal(priced.shipping())
                .setVatIncluded(priced.vatIncluded())
                .setTotal(priced.total())
                .setStatus(OrderStatus.PENDING_PAYMENT)
                .setReservedUntil(now.plusMinutes(paymentWindowMinutes))
                .setCreatedAt(now)
                .setUpdatedAt(now)
                .build());
        for (CartLine line : priced.items()) {
            orderItemRepository.save(new OrderItem.Builder()
                    .setOrderId(order.getOrderId())
                    .setCookieId(line.cookieId())
                    .setVariantId(line.variantId())
                    .setCookieName(line.name())
                    .setVariantLabel(line.variantLabel())
                    .setUnitsPerPack(line.unitsPerPack())
                    .setUnitPrice(line.unitPrice())
                    .setQuantity(line.quantity())
                    .setLineTotal(line.lineTotal())
                    .build());
        }
        orderService.addEvent(order, OrderStatus.PENDING_PAYMENT, "Order placed", "customer");
        cartRepository.save(new Cart.Builder().copy(cart).setStatus(CartStatus.CHECKED_OUT).setUpdatedAt(now).build());
        if (Boolean.TRUE.equals(request.newsletterOptIn())) {
            newsletterService.subscribe(contact.email(), "checkout");
        }

        String token = orderLinks.token(order);
        if (order.getTotal().signum() == 0) {
            orderService.markPaid(orderNumber, "system", "Nothing to pay");
            return new CheckoutResponse(orderNumber, token, OrderStatus.PAID, order.getTotal(), null);
        }
        return new CheckoutResponse(orderNumber, token, OrderStatus.PENDING_PAYMENT, order.getTotal(),
                paymentGateway.redirectFor(order));
    }

    private record Contact(String email, String firstName, String lastName, String phone) {
    }

    private Contact validContact(CheckoutRequest request, String signedInEmail) {
        if (Helper.isNullOrEmpty(request.cartId())) {
            throw new BadRequestException("Your cart is missing: add something to it first");
        }
        if (request.shippingMethod() == null) {
            throw new BadRequestException("Choose how you'd like to get your order");
        }
        String email = signedInEmail != null ? signedInEmail : Helper.cleanEmail(request.email());
        if (!Helper.isValidEmail(email)) {
            throw new BadRequestException("Enter a valid email address");
        }
        String firstName = Helper.clean(request.firstName());
        String lastName = Helper.clean(request.lastName());
        if (!Helper.isPresent(firstName, MAX_NAME_LENGTH) || !Helper.isPresent(lastName, MAX_NAME_LENGTH)) {
            throw new BadRequestException("Enter your first and last name");
        }
        String phone = Helper.clean(request.phone());
        boolean phoneRequired = request.shippingMethod() != ShippingMethod.PICKUP;
        if (Helper.isNullOrEmpty(phone) ? phoneRequired : !Helper.isValidMobileNumber(phone)) {
            throw new BadRequestException("Enter a South African mobile number so the courier can reach you");
        }
        return new Contact(email, firstName, lastName, Helper.isNullOrEmpty(phone) ? null : phone);
    }

    private static Address validAddress(AddressRequest request) {
        if (request == null) {
            throw new BadRequestException("Enter your delivery address");
        }
        String streetNumber = Helper.clean(request.streetNumber());
        String streetName = Helper.clean(request.streetName());
        String suburb = Helper.clean(request.suburb());
        String city = Helper.clean(request.city());
        String postalCode = Helper.clean(request.postalCode());
        if (!Helper.isPresent(streetNumber, MAX_STREET_NUMBER_LENGTH)
                || !Helper.isPresent(streetName, MAX_ADDRESS_PART_LENGTH)
                || !Helper.isPresent(suburb, MAX_ADDRESS_PART_LENGTH)
                || !Helper.isPresent(city, MAX_ADDRESS_PART_LENGTH)
                || request.province() == null) {
            throw new BadRequestException("Enter your street number and name, suburb, city and province");
        }
        if (!Helper.isValidPostalCode(postalCode)) {
            throw new BadRequestException("Enter a 4-digit postal code");
        }
        return new Address.Builder()
                .setStreetNumber(streetNumber)
                .setStreetName(streetName)
                .setComplex(optionalText(request.complex(), MAX_ADDRESS_PART_LENGTH, "Unit or complex"))
                .setSuburb(suburb)
                .setCity(city)
                .setProvince(request.province())
                .setPostalCode(postalCode)
                .build();
    }

    private static String validLocker(String locker) {
        String cleaned = Helper.clean(locker);
        if (!Helper.isPresent(cleaned, MAX_LOCKER_LENGTH)) {
            throw new BadRequestException("Choose the PUDO locker you'd like to collect from");
        }
        return cleaned;
    }

    private Cart lockCheckoutCart(String cartId, String signedInEmail) {
        Cart cart = cartRepository.findForUpdate(cartId)
                .filter(found -> !found.isAccountOwned() || Objects.equals(found.getCustomerEmail(), signedInEmail))
                .orElseThrow(() -> new NotFoundException("Cart not found"));
        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new ConflictException("This cart has already been checked out");
        }
        return cart;
    }

    private void requireCheckoutable(CartView priced, String email) {
        if (priced.items().isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }
        if (priced.belowMinimum()) {
            throw new ConflictException("Orders start at R" + priced.minimumOrder().toPlainString()
                    + " worth of cookies. Add R" + priced.minimumOrder().subtract(priced.subtotal()).toPlainString()
                    + " more to check out.");
        }
        if (!priced.canCheckout()) {
            throw new ConflictException("Some items in your cart have changed. Please review your cart.");
        }
        if (priced.discount() == null) {
            return;
        }
        if (!priced.discount().applied()) {
            throw new ConflictException(priced.discount().message() + ". Remove the code to continue.");
        }
        DiscountCode discount = discountRepository.findByCode(priced.discount().code()).orElseThrow();
        if (discount.isOnePerEmail() && orderRepository.existsByCustomerEmailAndDiscountCodeAndStatusIn(
                email, discount.getCode(), ONE_PER_EMAIL_STATUSES)) {
            throw new ConflictException("You've already used code " + discount.getCode());
        }
    }

    // Throwing rolls back the whole checkout, including stock already reserved for other cookies.
    private void reserveStock(CartView priced, String orderNumber) {
        Map<String, Integer> units = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        for (CartLine line : priced.items()) {
            units.merge(line.cookieId(), line.quantity() * line.unitsPerPack(), Integer::sum);
            names.put(line.cookieId(), line.name());
        }
        units.forEach((cookieId, needed) -> {
            if (!inventoryService.reserve(cookieId, needed, orderNumber)) {
                throw new ConflictException("Sorry, " + names.get(cookieId) + " just sold out. Please review your cart.");
            }
        });
    }

    // e.g. GFC-261024-7K3Q: dated, short enough to read over the phone, and not guessable in sequence.
    private String newOrderNumber() {
        String date = LocalDateTime.now().format(NUMBER_DATE);
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder suffix = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                suffix.append(NUMBER_ALPHABET.charAt(random.nextInt(NUMBER_ALPHABET.length())));
            }
            String number = "GFC-" + date + "-" + suffix;
            if (!orderRepository.existsByOrderNumber(number)) {
                return number;
            }
        }
        throw new IllegalStateException("Could not find a free order number");
    }

    private static String optionalText(String value, int max, String field) {
        String cleaned = Helper.clean(value);
        if (!Helper.isWithin(cleaned, max)) {
            throw new BadRequestException(field + " must be at most " + max + " characters");
        }
        return Helper.isNullOrEmpty(cleaned) ? null : cleaned;
    }
}
