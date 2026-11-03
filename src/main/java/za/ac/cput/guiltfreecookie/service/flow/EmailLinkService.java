package za.ac.cput.guiltfreecookie.service.flow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Cart;
import za.ac.cput.guiltfreecookie.domain.CartStatus;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.OrderItem;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.dto.cart.AddItemRequest;
import za.ac.cput.guiltfreecookie.dto.cart.CartView;
import za.ac.cput.guiltfreecookie.dto.engagement.ReviewRequest;
import za.ac.cput.guiltfreecookie.dto.engagement.ReviewView;
import za.ac.cput.guiltfreecookie.dto.flow.LinkedReviewRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;
import za.ac.cput.guiltfreecookie.service.cart.ShopCartService;
import za.ac.cput.guiltfreecookie.service.engagement.ReviewService;
import za.ac.cput.guiltfreecookie.service.order.OrderLinks;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.util.List;

// What the links in flow emails do: reopen a saved cart, refill a past order, review a delivered one.
@Service
public class EmailLinkService {

    static final String LINK_EXPIRED = "This link has expired. Your cart may have been ordered or cleared.";

    private final CartRepository cartRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final ShopCartService shopCartService;
    private final ReviewService reviewService;
    private final OrderLinks orderLinks;
    private final MarketingLinks marketingLinks;
    private final MarketingConsent consent;

    public EmailLinkService(CartRepository cartRepository, CustomerOrderRepository orderRepository,
                            OrderItemRepository itemRepository, ShopCartService shopCartService,
                            ReviewService reviewService, OrderLinks orderLinks, MarketingLinks marketingLinks,
                            MarketingConsent consent) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.shopCartService = shopCartService;
        this.reviewService = reviewService;
        this.orderLinks = orderLinks;
        this.marketingLinks = marketingLinks;
        this.consent = consent;
    }

    // Account carts aren't recoverable by link; their owner signs in instead.
    @Transactional(readOnly = true)
    public CartView recoverCart(String token) {
        Cart cart = Helper.isNullOrEmpty(token) ? null : cartRepository.findByRecoveryToken(token)
                .filter(found -> found.getStatus() == CartStatus.ACTIVE && !found.isAccountOwned())
                .orElse(null);
        if (cart == null) {
            throw new NotFoundException(LINK_EXPIRED);
        }
        return shopCartService.get(cart.getCartId());
    }

    // Adds whatever from the order is still in stock to the shopper's current guest cart, or a new one.
    @Transactional
    public CartView reorder(String orderNumber, String token, String currentCartId) {
        CustomerOrder order = linkedOrder(orderNumber, token);
        List<OrderItem> items = itemRepository.findByOrderId(order.getOrderId());
        String cartId = Helper.isNullOrEmpty(currentCartId) ? null : cartRepository.findById(currentCartId)
                .filter(cart -> cart.getStatus() == CartStatus.ACTIVE && !cart.isAccountOwned())
                .map(Cart::getCartId).orElse(null);
        if (cartId == null) {
            cartId = shopCartService.create(null).cartId();
        }
        int added = 0;
        for (OrderItem item : items) {
            try {
                shopCartService.addItem(cartId, new AddItemRequest(item.getVariantId(), item.getQuantity()));
                added++;
            } catch (ConflictException | BadRequestException | NotFoundException e) {
                // Sold out, retired or over the stock we have: leave it out.
            }
        }
        if (added == 0) {
            throw new ConflictException("None of the cookies from that order are available right now");
        }
        CartView cart = shopCartService.get(cartId);
        return cart.email() == null ? shopCartService.setEmail(cartId, order.getCustomerEmail()) : cart;
    }

    @Transactional
    public ReviewView review(LinkedReviewRequest request) {
        CustomerOrder order = linkedOrder(request.orderNumber(), request.token());
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ConflictException("You can review your cookies once they've arrived");
        }
        boolean inOrder = itemRepository.findByOrderId(order.getOrderId()).stream()
                .anyMatch(item -> item.getCookieId().equals(request.cookieId()));
        if (!inOrder) {
            throw new BadRequestException("That cookie isn't part of this order");
        }
        return reviewService.createFromOrderLink(order.getCustomerEmail(), request.cookieId(),
                new ReviewRequest(request.rating(), request.title(), request.body(), request.authorName()));
    }

    @Transactional
    public void unsubscribe(String email, String token, String reason) {
        consent.unsubscribe(verified(email, token), reason);
    }

    @Transactional
    public void resubscribe(String email, String token) {
        consent.resubscribe(verified(email, token), "resubscribe");
    }

    // "Keep me subscribed" from the sunset emails.
    @Transactional
    public void stay(String email, String token) {
        String verified = verified(email, token);
        if (!consent.isUnsubscribed(verified)) {
            consent.resubscribe(verified, "kept");
            consent.markEngaged(verified);
        }
    }

    private String verified(String email, String token) {
        String cleaned = Helper.cleanEmail(email);
        if (!marketingLinks.matches(cleaned, token)) {
            throw new BadRequestException("This link isn't valid. Use the link from your most recent email.");
        }
        return cleaned;
    }

    private CustomerOrder linkedOrder(String orderNumber, String token) {
        CustomerOrder order = Helper.isNullOrEmpty(orderNumber) ? null
                : orderRepository.findByOrderNumber(orderNumber).filter(found -> orderLinks.matches(found, token)).orElse(null);
        if (order == null || !order.getStatus().isPaid()) {
            throw new NotFoundException("Order not found");
        }
        return order;
    }
}
