package za.ac.cput.guiltfreecookie.service.order;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.OrderItem;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;
import za.ac.cput.guiltfreecookie.service.engagement.SiteInfoService;
import za.ac.cput.guiltfreecookie.service.shipping.ShippingSettingsService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// PDFs for a paid order: the A4 invoice and the till-slip receipt, from templates/documents/.
@Component
public class OrderDocuments {

    public enum Kind {
        INVOICE("invoice"),
        RECEIPT("receipt");

        private final String slug;

        Kind(String slug) {
            this.slug = slug;
        }
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private final OrderItemRepository itemRepository;
    private final ShippingSettingsService shippingSettingsService;
    private final SiteInfoService siteInfoService;
    private final OrderLinks orderLinks;
    private final SpringTemplateEngine engine;
    private final String publicBase;
    private final boolean vatRegistered;
    private final String vatNumber;
    private final BigDecimal vatRate;

    public OrderDocuments(OrderItemRepository itemRepository, ShippingSettingsService shippingSettingsService,
                          SiteInfoService siteInfoService, OrderLinks orderLinks, SpringTemplateEngine engine,
                          @Value("${app.public-base-url}") String publicBase,
                          @Value("${app.vat.registered}") boolean vatRegistered,
                          @Value("${app.vat.number:}") String vatNumber,
                          @Value("${app.vat.rate}") BigDecimal vatRate) {
        this.itemRepository = itemRepository;
        this.shippingSettingsService = shippingSettingsService;
        this.siteInfoService = siteInfoService;
        this.orderLinks = orderLinks;
        this.engine = engine;
        this.publicBase = publicBase.replaceAll("/+$", "");
        this.vatRegistered = vatRegistered;
        this.vatNumber = vatNumber;
        this.vatRate = vatRate;
    }

    // Refunded orders keep their documents: they record the original sale.
    public static boolean available(CustomerOrder order) {
        return order.getStatus().isPaid() || order.getStatus() == OrderStatus.REFUNDED;
    }

    public String url(CustomerOrder order, Kind kind) {
        return publicBase + "/api/v1/orders/" + order.getOrderNumber() + "/" + kind.slug + "?token=" + orderLinks.token(order);
    }

    public String fileName(CustomerOrder order, Kind kind) {
        return "GuiltFreeCookie-" + kind.slug + "-" + order.getOrderNumber() + ".pdf";
    }

    public byte[] pdf(CustomerOrder order, Kind kind) {
        if (!available(order)) {
            throw new ConflictException("The " + kind.slug + " is ready once the order is paid");
        }
        String html = engine.process("documents/order-" + kind.slug, new Context(Locale.ENGLISH, model(order)));
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            new PdfRendererBuilder()
                    .withHtmlContent(html, null)
                    .toStream(out)
                    .run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Map<String, Object> model(CustomerOrder order) {
        List<OrderItem> items = itemRepository.findByOrderId(order.getOrderId());
        boolean discounted = order.getDiscountTotal().signum() != 0;
        Map<String, Object> model = new HashMap<>();
        model.put("site", siteInfoService);
        model.put("title", vatRegistered ? "Tax invoice" : "Invoice");
        model.put("vatNumber", vatRegistered && !vatNumber.isBlank() ? vatNumber : null);
        model.put("orderNumber", order.getOrderNumber());
        model.put("invoiceDate", DATE.format(order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt()));
        model.put("paidAt", DATE_TIME.format(order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt()));
        model.put("refunded", order.getStatus() == OrderStatus.REFUNDED);
        model.put("customerName", order.getFirstName() + " " + order.getLastName());
        model.put("customerEmail", order.getCustomerEmail());
        model.put("customerPhone", order.getPhone());
        model.put("delivery", order.getShippingMethod() == ShippingMethod.PICKUP
                ? "Collect: " + OrderViews.pickupDetails(shippingSettingsService.get())
                : OrderEmailListener.deliveryDetails(order));
        model.put("items", items.stream().map(item -> Map.of(
                "name", item.getCookieName(),
                "variant", item.getVariantLabel(),
                "quantity", item.getQuantity(),
                "unitPrice", OrderEmailListener.rand(item.getUnitPrice()),
                "lineTotal", OrderEmailListener.rand(item.getLineTotal()))).toList());
        model.put("subtotal", OrderEmailListener.rand(order.getSubtotal()));
        model.put("discountCode", order.getDiscountCode());
        model.put("discount", discounted ? "- " + OrderEmailListener.rand(order.getDiscountTotal()) : null);
        model.put("shipping", order.getShippingTotal().signum() == 0 ? "Free" : OrderEmailListener.rand(order.getShippingTotal()));
        model.put("vat", order.getVatIncluded() == null ? null : OrderEmailListener.rand(order.getVatIncluded()));
        model.put("vatPercent", vatRate.movePointRight(2).stripTrailingZeros().toPlainString());
        model.put("total", OrderEmailListener.rand(order.getTotal()));
        // A till slip is as long as its contents, and PDF pages need a fixed size up front.
        model.put("receiptHeightMm", 104 + 11 * items.size() + (discounted ? 5 : 0) + (order.getVatIncluded() != null ? 5 : 0));
        return model;
    }
}
