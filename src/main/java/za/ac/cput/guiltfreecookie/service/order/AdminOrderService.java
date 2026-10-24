package za.ac.cput.guiltfreecookie.service.order;

import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.InventoryReason;
import za.ac.cput.guiltfreecookie.domain.OrderItem;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.dto.catalog.PageResponse;
import za.ac.cput.guiltfreecookie.dto.order.AdminOrderView;
import za.ac.cput.guiltfreecookie.dto.order.OrderSummary;
import za.ac.cput.guiltfreecookie.dto.order.StatusChangeRequest;
import za.ac.cput.guiltfreecookie.dto.order.TrackingRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;
import za.ac.cput.guiltfreecookie.repository.OrderStatusEventRepository;
import za.ac.cput.guiltfreecookie.repository.PaymentRepository;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminOrderService {

    static final int MAX_PAGE_SIZE = 100;
    static final int MAX_NOTE_LENGTH = 500;
    static final int MAX_TRACKING_LENGTH = 100;
    static final int MAX_URL_LENGTH = 500;

    // Status moves an admin can make directly. Shipping uses ship(), refunds use refund().
    static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.CANCELLED),
            OrderStatus.PAID, EnumSet.of(OrderStatus.PROCESSING),
            OrderStatus.PROCESSING, EnumSet.of(OrderStatus.READY_FOR_PICKUP),
            OrderStatus.READY_FOR_PICKUP, EnumSet.of(OrderStatus.DELIVERED),
            OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED));
    // Refunding before the cookies leave the bakery puts them back on the shelf.
    private static final Set<OrderStatus> RESTOCK_ON_REFUND =
            EnumSet.of(OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.READY_FOR_PICKUP);

    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderStatusEventRepository eventRepository;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final OrderViews orderViews;
    private final ApplicationEventPublisher events;

    public AdminOrderService(CustomerOrderRepository orderRepository, OrderItemRepository itemRepository,
                             OrderStatusEventRepository eventRepository, PaymentRepository paymentRepository,
                             OrderService orderService, OrderViews orderViews, ApplicationEventPublisher events) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.eventRepository = eventRepository;
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.orderViews = orderViews;
        this.events = events;
    }

    // q matches order number, email or last name. Dates are inclusive. Newest first.
    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> search(String q, OrderStatus status, LocalDate from, LocalDate to, Integer page, Integer size) {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 20 : size;
        if (pageNumber < 0 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BadRequestException("page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE);
        }
        Page<CustomerOrder> results = orderRepository.findAll(specFor(q, status, from, to),
                PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Order.desc("createdAt"))));
        Map<String, List<OrderItem>> items = itemRepository
                .findByOrderIdIn(results.getContent().stream().map(CustomerOrder::getOrderId).toList())
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
        return PageResponse.of(results, orders -> orders.stream()
                .map(order -> orderViews.summary(order, items.getOrDefault(order.getOrderId(), List.of())))
                .toList());
    }

    @Transactional(readOnly = true)
    public AdminOrderView get(String orderNumber) {
        return adminView(orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException(OrderService.NOT_FOUND)));
    }

    @Transactional
    public AdminOrderView changeStatus(String orderNumber, StatusChangeRequest request, String adminId) {
        CustomerOrder order = orderService.lock(orderNumber);
        OrderStatus target = request.status();
        if (target == null || !ALLOWED.getOrDefault(order.getStatus(), Set.of()).contains(target)) {
            throw new ConflictException("An order that is " + label(order.getStatus()) + " can't be marked " + label(target));
        }
        if (target == OrderStatus.READY_FOR_PICKUP && order.getShippingMethod() != ShippingMethod.PICKUP) {
            throw new ConflictException("Only pickup orders can be ready for pickup: ship this one with a tracking number");
        }
        String note = note(request.note());
        if (target == OrderStatus.CANCELLED) {
            orderService.releaseStock(order, InventoryReason.ORDER_RELEASED);
        }
        CustomerOrder updated = orderService.changeStatus(order, target, note, adminId);
        if (target == OrderStatus.READY_FOR_PICKUP) {
            events.publishEvent(new OrderEvents.ReadyForPickup(orderNumber));
        }
        return adminView(updated);
    }

    // First call marks the order shipped and emails the customer; later calls just correct the tracking details.
    @Transactional
    public AdminOrderView ship(String orderNumber, TrackingRequest request, String adminId) {
        CustomerOrder order = orderService.lock(orderNumber);
        if (order.getShippingMethod() == ShippingMethod.PICKUP) {
            throw new ConflictException("This is a pickup order: mark it ready for pickup instead");
        }
        if (!EnumSet.of(OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED).contains(order.getStatus())) {
            throw new ConflictException("An order that is " + label(order.getStatus()) + " can't be shipped");
        }
        String courier = Helper.clean(request.courierName());
        String trackingNumber = Helper.clean(request.trackingNumber());
        String trackingUrl = Helper.clean(request.trackingUrl());
        if (!Helper.isPresent(courier, MAX_TRACKING_LENGTH) || !Helper.isPresent(trackingNumber, MAX_TRACKING_LENGTH)) {
            throw new BadRequestException("Enter the courier and tracking number");
        }
        if (!Helper.isNullOrEmpty(trackingUrl) && (!trackingUrl.startsWith("https://") || trackingUrl.length() > MAX_URL_LENGTH)) {
            throw new BadRequestException("The tracking link must start with https://");
        }
        boolean firstShipment = order.getStatus() != OrderStatus.SHIPPED;
        CustomerOrder updated = orderRepository.save(new CustomerOrder.Builder()
                .copy(order)
                .setStatus(OrderStatus.SHIPPED)
                .setCourierName(courier)
                .setTrackingNumber(trackingNumber)
                .setTrackingUrl(Helper.isNullOrEmpty(trackingUrl) ? null : trackingUrl)
                .setUpdatedAt(LocalDateTime.now())
                .build());
        orderService.addEvent(updated, OrderStatus.SHIPPED,
                (firstShipment ? "Shipped with " : "Tracking updated: ") + courier + " " + trackingNumber, adminId);
        if (firstShipment) {
            events.publishEvent(new OrderEvents.Shipped(orderNumber));
        }
        return adminView(updated);
    }

    // Records the refund; the money itself is paid back from the PayFast dashboard.
    @Transactional
    public AdminOrderView refund(String orderNumber, String rawNote, String adminId) {
        CustomerOrder order = orderService.lock(orderNumber);
        if (!order.getStatus().isPaid()) {
            throw new ConflictException("Only a paid order can be refunded");
        }
        boolean restocked = RESTOCK_ON_REFUND.contains(order.getStatus());
        if (restocked) {
            orderService.releaseStock(order, InventoryReason.REFUNDED);
        }
        List<String> notes = new ArrayList<>();
        if (rawNote != null && !rawNote.isBlank()) {
            notes.add(note(rawNote));
        }
        notes.add(restocked ? "Cookies returned to stock." : "Already sent, so stock is unchanged.");
        notes.add("Pay the refund in the PayFast dashboard.");
        return adminView(orderService.changeStatus(order, OrderStatus.REFUNDED, String.join(" ", notes), adminId));
    }

    private AdminOrderView adminView(CustomerOrder order) {
        return new AdminOrderView(
                orderViews.view(order),
                order.getCustomerNote(),
                eventRepository.findByOrderIdOrderByCreatedAtAsc(order.getOrderId()),
                paymentRepository.findByOrderIdOrderByReceivedAtAsc(order.getOrderId()));
    }

    private static String note(String note) {
        String cleaned = Helper.clean(note);
        if (!Helper.isWithin(cleaned, MAX_NOTE_LENGTH)) {
            throw new BadRequestException("Note must be at most " + MAX_NOTE_LENGTH + " characters");
        }
        return Helper.isNullOrEmpty(cleaned) ? null : cleaned;
    }

    private static String label(OrderStatus status) {
        return status == null ? "that" : status.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static Specification<CustomerOrder> specFor(String q, OrderStatus status, LocalDate from, LocalDate to) {
        String search = q == null ? null : q.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isEmpty()) {
                String pattern = "%" + search.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), pattern, '\\'),
                        cb.like(cb.lower(root.get("customerEmail")), pattern, '\\'),
                        cb.like(cb.lower(root.get("lastName")), pattern, '\\')));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
