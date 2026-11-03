package za.ac.cput.guiltfreecookie.service.order;

import org.springframework.stereotype.Component;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.OrderItem;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.domain.ShippingSettings;
import za.ac.cput.guiltfreecookie.dto.order.OrderLine;
import za.ac.cput.guiltfreecookie.dto.order.OrderSummary;
import za.ac.cput.guiltfreecookie.dto.order.OrderView;
import za.ac.cput.guiltfreecookie.dto.order.TimelineEntry;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;
import za.ac.cput.guiltfreecookie.repository.OrderStatusEventRepository;
import za.ac.cput.guiltfreecookie.service.shipping.ShippingSettingsService;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderViews {

    private final OrderItemRepository itemRepository;
    private final OrderStatusEventRepository eventRepository;
    private final ShippingSettingsService shippingSettingsService;

    public OrderViews(OrderItemRepository itemRepository, OrderStatusEventRepository eventRepository,
                      ShippingSettingsService shippingSettingsService) {
        this.itemRepository = itemRepository;
        this.eventRepository = eventRepository;
        this.shippingSettingsService = shippingSettingsService;
    }

    public OrderView view(CustomerOrder order) {
        ShippingSettings settings = shippingSettingsService.get();
        boolean awaitingPayment = order.getStatus() == OrderStatus.PENDING_PAYMENT
                && order.getReservedUntil() != null && LocalDateTime.now().isBefore(order.getReservedUntil());
        List<TimelineEntry> timeline = eventRepository.findByOrderIdOrderByCreatedAtAsc(order.getOrderId()).stream()
                .map(event -> new TimelineEntry(event.getStatus(), event.getNote(), event.getCreatedAt()))
                .toList();
        return new OrderView(
                order.getOrderNumber(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getCustomerEmail(),
                order.getFirstName(),
                order.getLastName(),
                order.getPhone(),
                order.getShippingMethod(),
                order.getAddress(),
                order.getPudoLocker(),
                order.getShippingMethod() == ShippingMethod.PICKUP ? pickupDetails(settings) : null,
                eta(order.getShippingMethod(), settings),
                lines(order),
                order.getSubtotal(),
                order.getDiscountCode(),
                order.getDiscountTotal(),
                order.getShippingTotal(),
                order.getVatIncluded(),
                order.getTotal(),
                order.getCourierName(),
                order.getTrackingNumber(),
                order.getTrackingUrl(),
                timeline,
                awaitingPayment ? order.getReservedUntil() : null,
                awaitingPayment,
                order.getStatus() == OrderStatus.PENDING_PAYMENT,
                order.getStatus().isPaid() || order.getStatus() == OrderStatus.REFUNDED ? null : order.getCartId());
    }

    public OrderSummary summary(CustomerOrder order, List<OrderItem> items) {
        int itemCount = items.stream().mapToInt(OrderItem::getQuantity).sum();
        return new OrderSummary(order.getOrderNumber(), order.getCreatedAt(),
                order.getFirstName() + " " + order.getLastName(), order.getCustomerEmail(), order.getShippingMethod(),
                itemCount, order.getTotal(), order.getStatus());
    }

    public List<OrderLine> lines(CustomerOrder order) {
        return itemRepository.findByOrderId(order.getOrderId()).stream()
                .map(item -> new OrderLine(item.getCookieId(), item.getCookieName(), item.getVariantLabel(),
                        item.getUnitsPerPack(), item.getUnitPrice(), item.getQuantity(), item.getLineTotal()))
                .toList();
    }

    public static String pickupDetails(ShippingSettings settings) {
        if (settings.getPickupInstructions() == null) {
            return settings.getPickupAddress();
        }
        return settings.getPickupAddress() + "\n" + settings.getPickupInstructions();
    }

    private static String eta(ShippingMethod method, ShippingSettings settings) {
        return switch (method) {
            case COURIER -> settings.getCourierEta();
            case PUDO -> settings.getPudoEta();
            case PICKUP -> settings.getPickupEta();
        };
    }
}
