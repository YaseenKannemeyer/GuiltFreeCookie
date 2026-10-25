package za.ac.cput.guiltfreecookie.service.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Cart;
import za.ac.cput.guiltfreecookie.domain.CartStatus;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.InventoryReason;
import za.ac.cput.guiltfreecookie.domain.OrderItem;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.OrderStatusEvent;
import za.ac.cput.guiltfreecookie.dto.order.OrderView;
import za.ac.cput.guiltfreecookie.dto.order.PaymentRedirect;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;
import za.ac.cput.guiltfreecookie.repository.OrderStatusEventRepository;
import za.ac.cput.guiltfreecookie.service.catalog.InventoryService;
import za.ac.cput.guiltfreecookie.service.discount.DiscountService;
import za.ac.cput.guiltfreecookie.service.payment.PaymentGateway;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Customer-facing order actions and the payment outcomes that move an order on.
@Service
public class OrderService {

    static final String NOT_FOUND = "Order not found";

    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderStatusEventRepository eventRepository;
    private final CartRepository cartRepository;
    private final InventoryService inventoryService;
    private final DiscountService discountService;
    private final PaymentGateway paymentGateway;
    private final OrderLinks orderLinks;
    private final OrderViews orderViews;
    private final ApplicationEventPublisher events;
    private final long paymentWindowMinutes;

    public OrderService(CustomerOrderRepository orderRepository, OrderItemRepository itemRepository,
                        OrderStatusEventRepository eventRepository, CartRepository cartRepository,
                        InventoryService inventoryService, DiscountService discountService,
                        PaymentGateway paymentGateway, OrderLinks orderLinks, OrderViews orderViews,
                        ApplicationEventPublisher events,
                        @Value("${app.order.payment-window-minutes}") long paymentWindowMinutes) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.eventRepository = eventRepository;
        this.cartRepository = cartRepository;
        this.inventoryService = inventoryService;
        this.discountService = discountService;
        this.paymentGateway = paymentGateway;
        this.orderLinks = orderLinks;
        this.orderViews = orderViews;
        this.events = events;
        this.paymentWindowMinutes = paymentWindowMinutes;
    }

    @Transactional(readOnly = true)
    public OrderView view(String orderNumber, String token) {
        return orderViews.view(requireWithToken(orderNumber, token));
    }

    // "Pay now" again, e.g. after closing the PayFast page, while the stock is still held.
    @Transactional(readOnly = true)
    public PaymentRedirect paymentRedirect(String orderNumber, String token) {
        CustomerOrder order = requireWithToken(orderNumber, token);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT || !LocalDateTime.now().isBefore(order.getReservedUntil())) {
            throw new ConflictException("This order can't be paid any more. Your cart is ready to check out again.");
        }
        return paymentGateway.redirectFor(order);
    }

    // The customer cancelled on the PayFast page: free the stock and give them their cart back.
    @Transactional
    public OrderView cancelUnpaid(String orderNumber, String token) {
        requireWithToken(orderNumber, token);
        CustomerOrder order = lock(orderNumber);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("Only an unpaid order can be cancelled");
        }
        releaseHold(order);
        CustomerOrder cancelled = changeStatus(order, OrderStatus.CANCELLED, "Cancelled before payment", "customer");
        reopenCart(cancelled);
        return orderViews.view(cancelled);
    }

    // Safe to call more than once: PayFast can repeat a notification.
    @Transactional
    public void markPaid(String orderNumber, String actor, String note) {
        CustomerOrder order = lock(orderNumber);
        if (order.getStatus().isPaid() || order.getStatus() == OrderStatus.REFUNDED) {
            return;
        }
        List<String> notes = new ArrayList<>(List.of(note));
        // The code's use was taken at checkout; a late payment on a released order has to take it again.
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.PAYMENT_FAILED) {
            if (!reserveStock(order)) {
                notes.add("The stock had already been released and isn't available now: refund or substitute.");
            }
            if (order.getDiscountCode() != null && !discountService.redeem(order.getDiscountCode())) {
                notes.add("Discount code " + order.getDiscountCode() + " had reached its use limit.");
            }
            closeCart(order);
        }
        orderRepository.save(new CustomerOrder.Builder()
                .copy(order)
                .setStatus(OrderStatus.PAID)
                .setPaidAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now())
                .build());
        addEvent(order, OrderStatus.PAID, String.join(" ", notes), actor);
        events.publishEvent(new OrderEvents.Paid(orderNumber));
    }

    @Transactional
    public void markPaymentFailed(String orderNumber, String actor, String note) {
        CustomerOrder order = lock(orderNumber);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }
        releaseHold(order);
        reopenCart(changeStatus(order, OrderStatus.PAYMENT_FAILED, note, actor));
    }

    // Unpaid orders past their payment window give their stock back and reopen the cart.
    @Transactional
    public int releaseExpired() {
        int released = 0;
        for (CustomerOrder expired : orderRepository.findByStatusAndReservedUntilBefore(OrderStatus.PENDING_PAYMENT, LocalDateTime.now())) {
            CustomerOrder order = lock(expired.getOrderNumber());
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                continue;
            }
            releaseHold(order);
            reopenCart(changeStatus(order, OrderStatus.CANCELLED,
                    "Payment wasn't completed within " + paymentWindowMinutes + " minutes", "system"));
            released++;
        }
        return released;
    }

    CustomerOrder lock(String orderNumber) {
        return orderRepository.findForUpdate(orderNumber).orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    CustomerOrder changeStatus(CustomerOrder order, OrderStatus status, String note, String actor) {
        CustomerOrder saved = orderRepository.save(new CustomerOrder.Builder()
                .copy(order)
                .setStatus(status)
                .setUpdatedAt(LocalDateTime.now())
                .build());
        addEvent(saved, status, note, actor);
        return saved;
    }

    void addEvent(CustomerOrder order, OrderStatus status, String note, String actor) {
        eventRepository.save(new OrderStatusEvent.Builder()
                .setOrderId(order.getOrderId())
                .setStatus(status)
                .setNote(note)
                .setActor(actor)
                .setCreatedAt(LocalDateTime.now())
                .build());
    }

    // An unpaid order letting go: its stock, and the discount-code use it was holding.
    void releaseHold(CustomerOrder order) {
        releaseStock(order, InventoryReason.ORDER_RELEASED);
        if (order.getDiscountCode() != null) {
            discountService.release(order.getDiscountCode());
        }
    }

    void releaseStock(CustomerOrder order, InventoryReason reason) {
        unitsByCookie(order).forEach((cookieId, units) ->
                inventoryService.release(cookieId, units, order.getOrderNumber(), reason));
    }

    // All or nothing: if one cookie can't be reserved, the ones already taken go back.
    private boolean reserveStock(CustomerOrder order) {
        Map<String, Integer> reserved = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : unitsByCookie(order).entrySet()) {
            if (!inventoryService.reserve(entry.getKey(), entry.getValue(), order.getOrderNumber())) {
                reserved.forEach((cookieId, units) ->
                        inventoryService.release(cookieId, units, order.getOrderNumber(), InventoryReason.ORDER_RELEASED));
                return false;
            }
            reserved.put(entry.getKey(), entry.getValue());
        }
        return true;
    }

    private Map<String, Integer> unitsByCookie(CustomerOrder order) {
        Map<String, Integer> units = new LinkedHashMap<>();
        for (OrderItem item : itemRepository.findByOrderId(order.getOrderId())) {
            units.merge(item.getCookieId(), item.getQuantity() * item.getUnitsPerPack(), Integer::sum);
        }
        return units;
    }

    boolean holdsStock(CustomerOrder order) {
        return inventoryService.isHeldBy(order.getOrderNumber());
    }

    void reopenCart(CustomerOrder order) {
        setCartStatus(order, CartStatus.CHECKED_OUT, CartStatus.ACTIVE);
    }

    private void closeCart(CustomerOrder order) {
        setCartStatus(order, CartStatus.ACTIVE, CartStatus.CHECKED_OUT);
    }

    private void setCartStatus(CustomerOrder order, CartStatus from, CartStatus to) {
        if (order.getCartId() == null) {
            return;
        }
        cartRepository.findForUpdate(order.getCartId())
                .filter(cart -> cart.getStatus() == from)
                .ifPresent(cart -> cartRepository.save(new Cart.Builder()
                        .copy(cart).setStatus(to).setUpdatedAt(LocalDateTime.now()).build()));
    }

    // Same answer for a wrong token as for a missing order, so order numbers can't be probed.
    private CustomerOrder requireWithToken(String orderNumber, String token) {
        return orderRepository.findByOrderNumber(orderNumber)
                .filter(order -> orderLinks.matches(order, token))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }
}
