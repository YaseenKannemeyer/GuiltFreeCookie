package za.ac.cput.guiltfreecookie.service.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import za.ac.cput.guiltfreecookie.domain.Address;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.OrderItem;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;
import za.ac.cput.guiltfreecookie.service.engagement.SiteInfoService;
import za.ac.cput.guiltfreecookie.service.notification.EmailService;
import za.ac.cput.guiltfreecookie.service.shipping.ShippingSettingsService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

// Order emails go out only after the change is committed, so nobody is told about an order that rolled back.
// Models follow docs/handoff/phase-6-email-models.md.
@Component
public class OrderEmailListener {

    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final ShippingSettingsService shippingSettingsService;
    private final OrderLinks orderLinks;
    private final OrderDocuments orderDocuments;
    private final EmailService emailService;
    private final SiteInfoService siteInfoService;
    private final String frontendBase;

    public OrderEmailListener(CustomerOrderRepository orderRepository, OrderItemRepository itemRepository,
                              ShippingSettingsService shippingSettingsService, OrderLinks orderLinks,
                              OrderDocuments orderDocuments, EmailService emailService,
                              SiteInfoService siteInfoService, @Value("${app.frontend.base-url}") String frontendBase) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.shippingSettingsService = shippingSettingsService;
        this.orderLinks = orderLinks;
        this.orderDocuments = orderDocuments;
        this.emailService = emailService;
        this.siteInfoService = siteInfoService;
        this.frontendBase = frontendBase.replaceAll("/+$", "");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPaid(OrderEvents.Paid event) {
        orderRepository.findByOrderNumber(event.orderNumber()).ifPresent(order -> {
            Map<String, Object> model = base(order, "View your order", orderLinks.orderPage(order));
            List<OrderItem> items = itemRepository.findByOrderId(order.getOrderId());
            model.put("items", items.stream().map(item -> Map.of(
                    "name", item.getCookieName(),
                    "variant", item.getVariantLabel(),
                    "quantity", item.getQuantity(),
                    "lineTotal", rand(item.getLineTotal()))).toList());
            model.put("subtotal", rand(order.getSubtotal()));
            model.put("discount", order.getDiscountTotal().signum() == 0 ? null : "- " + rand(order.getDiscountTotal()));
            model.put("shipping", order.getShippingTotal().signum() == 0 ? "Free" : rand(order.getShippingTotal()));
            model.put("vat", order.getVatIncluded() == null ? null : rand(order.getVatIncluded()));
            model.put("total", rand(order.getTotal()));
            model.put("invoiceUrl", orderDocuments.url(order, OrderDocuments.Kind.INVOICE));
            emailService.sendTemplate(order.getCustomerEmail(), "Thank you! Order " + order.getOrderNumber() + " is confirmed",
                    "order-confirmation", model, false);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onShipped(OrderEvents.Shipped event) {
        orderRepository.findByOrderNumber(event.orderNumber()).ifPresent(order -> {
            String link = order.getTrackingUrl() != null ? order.getTrackingUrl() : orderLinks.orderPage(order);
            Map<String, Object> model = base(order, "Track your parcel", link);
            model.put("courierName", order.getCourierName());
            model.put("trackingNumber", order.getTrackingNumber());
            emailService.sendTemplate(order.getCustomerEmail(), "Your cookies are on their way (" + order.getOrderNumber() + ")",
                    "order-shipped", model, false);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onReadyForPickup(OrderEvents.ReadyForPickup event) {
        orderRepository.findByOrderNumber(event.orderNumber()).ifPresent(order -> emailService.sendTemplate(
                order.getCustomerEmail(), "Your order " + order.getOrderNumber() + " is ready to collect",
                "order-ready-for-pickup", base(order, "View your order", orderLinks.orderPage(order)), false));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCancelledByCustomer(OrderEvents.CancelledByCustomer event) {
        orderRepository.findByOrderNumber(event.orderNumber()).ifPresent(order -> {
            Map<String, Object> customer = base(order, "View your order", orderLinks.orderPage(order));
            customer.put("total", rand(order.getTotal()));
            emailService.sendTemplate(order.getCustomerEmail(), "Order " + order.getOrderNumber() + " is cancelled",
                    "order-cancelled", customer, false);

            Map<String, Object> shop = base(order, "Open the order", frontendBase + "/admindashboard/orders/" + order.getOrderNumber());
            shop.put("customerName", order.getFirstName() + " " + order.getLastName());
            shop.put("email", order.getCustomerEmail());
            shop.put("total", rand(order.getTotal()));
            emailService.sendTemplate(siteInfoService.setting("support-email"),
                    "Refund needed: " + order.getOrderNumber() + " was cancelled by the customer",
                    "order-cancelled-to-shop", shop, false);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPaidAfterCancellation(OrderEvents.PaidAfterCancellation event) {
        orderRepository.findByOrderNumber(event.orderNumber()).ifPresent(order -> {
            Map<String, Object> shop = base(order, "Open the order", frontendBase + "/admindashboard/orders/" + order.getOrderNumber());
            shop.put("customerName", order.getFirstName() + " " + order.getLastName());
            shop.put("email", order.getCustomerEmail());
            shop.put("total", rand(order.getTotal()));
            emailService.sendTemplate(siteInfoService.setting("support-email"),
                    "Refund needed: " + order.getOrderNumber() + " was paid after it was cancelled",
                    "order-paid-after-cancel-to-shop", shop, false);
        });
    }

    // HashMap rather than Map.of: delivery and pickup details are null for the other kind of order.
    private Map<String, Object> base(CustomerOrder order, String actionLabel, String actionUrl) {
        Map<String, Object> model = new HashMap<>();
        model.put("name", order.getFirstName());
        model.put("orderNumber", order.getOrderNumber());
        model.put("actionLabel", actionLabel);
        model.put("actionUrl", actionUrl);
        model.put("deliveryDetails", deliveryDetails(order));
        model.put("pickupDetails", order.getShippingMethod() == ShippingMethod.PICKUP
                ? OrderViews.pickupDetails(shippingSettingsService.get()) : null);
        return model;
    }

    static String deliveryDetails(CustomerOrder order) {
        if (order.getShippingMethod() == ShippingMethod.PUDO) {
            return "PUDO locker: " + order.getPudoLocker();
        }
        Address address = order.getAddress();
        if (address == null) {
            return null;
        }
        StringJoiner lines = new StringJoiner("\n");
        lines.add(order.getFirstName() + " " + order.getLastName());
        if (address.getComplex() != null) {
            lines.add(address.getComplex());
        }
        lines.add(address.getStreetNumber() + " " + address.getStreetName());
        lines.add(address.getSuburb());
        lines.add(address.getCity() + ", " + address.getPostalCode());
        return lines.toString();
    }

    static String rand(BigDecimal amount) {
        return "R " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
