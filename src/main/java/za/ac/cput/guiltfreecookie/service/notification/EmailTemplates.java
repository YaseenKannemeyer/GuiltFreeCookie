package za.ac.cput.guiltfreecookie.service.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.domain.EmailFlow;
import za.ac.cput.guiltfreecookie.service.engagement.SiteInfoService;
import za.ac.cput.guiltfreecookie.service.flow.FlowCatalog;
import za.ac.cput.guiltfreecookie.service.flow.FlowCopy;
import za.ac.cput.guiltfreecookie.service.flow.FlowData;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

// Renders templates/email/*.html. See docs/handoff/phase-6-email-models.md for each template's model.
@Service
public class EmailTemplates {

    // Flow emails all render through marketing-flow; each step is listed separately for previews.
    public static final String FLOW_TEMPLATE = "marketing-flow";
    public static final List<String> NAMES = Stream.concat(Stream.of(
            "password-reset",
            "contact-auto-reply",
            "contact-to-shop",
            "order-confirmation",
            "order-shipped",
            "order-ready-for-pickup",
            "claim-orders"), FlowCatalog.previewNames().stream()).toList();

    private final SpringTemplateEngine engine;
    private final SiteInfoService siteInfoService;
    private final String publicBase;
    private final String frontendBase;

    public EmailTemplates(
            SpringTemplateEngine engine,
            SiteInfoService siteInfoService,
            @Value("${app.public-base-url:http://localhost:8080/guiltFreeCookie}") String publicBase,
            @Value("${app.frontend.base-url}") String frontendBase) {
        this.engine = engine;
        this.siteInfoService = siteInfoService;
        this.publicBase = publicBase.replaceAll("/+$", "");
        this.frontendBase = frontendBase.replaceAll("/+$", "");
    }

    // site, publicBase, frontendBase and ctaVml are supplied here; callers shouldn't set them.
    public String render(String template, Map<String, Object> model) {
        if (!template.equals(FLOW_TEMPLATE)) {
            require(template);
        }
        Map<String, Object> values = new HashMap<>();
        values.put("site", siteInfoService);
        values.put("publicBase", publicBase);
        values.put("frontendBase", frontendBase);
        values.put("heroImage", publicBase + "/email/cookie.png");
        values.put("unsubscribeUrl", "");
        values.put("marketing", false);
        values.putAll(model);

        String actionUrl = Objects.toString(values.get("actionUrl"), frontendBase + "/products");
        if (!actionUrl.startsWith("https://") && !actionUrl.startsWith("http://")) {
            throw new BadRequestException("Email actionUrl must be an absolute HTTP URL");
        }
        String actionLabel = Objects.toString(values.get("actionLabel"), "Visit the bakery");
        values.put("actionUrl", actionUrl);
        values.put("actionLabel", actionLabel);
        values.put("ctaVml", outlookButton(actionUrl, actionLabel));
        String file = template.startsWith("flow-") ? FLOW_TEMPLATE : template;
        return engine.process("email/" + file, new Context(Locale.ENGLISH, values));
    }

    // Illustrative data for the admin previews. Never use it for real customers.
    public Map<String, Object> sample(String template) {
        require(template);
        if (template.startsWith("flow-")) {
            return flowSample(template);
        }
        Map<String, Object> model = new HashMap<>();
        model.put("name", "Sam");
        model.put("email", "sam@example.com");
        model.put("phone", "082 000 0000");
        model.put("subject", "A question about your cookies");
        model.put("message", "Could you tell me more about the ingredients in your chocolate cookies?");
        model.put("messageId", "preview-message");
        model.put("resetMinutes", 60);
        model.put("orderNumber", "GFC-PREVIEW");
        model.put("courierName", "Example courier");
        model.put("trackingNumber", "PREVIEW-123");
        model.put("deliveryDetails", "Example delivery: 12 Bakery Lane, Cape Town, 8001");
        if (template.equals("order-ready-for-pickup")) {
            model.put("pickupDetails",
                    "Example pickup: please bring your order number. We will confirm the collection address in your order.");
            model.put("deliveryDetails", null);
        }
        model.put("subtotal", "R 180.00");
        model.put("discount", "- R 18.00");
        model.put("shipping", "R 60.00");
        model.put("vat", "R 28.96");
        model.put("total", "R 222.00");
        model.put("items", List.of(Map.of(
                "name", "Double Chocolate Chunk",
                "variant", "Box of 6",
                "quantity", 1,
                "lineTotal", "R 180.00")));
        model.put("products", List.of(
                sampleProduct("Double Chocolate Chunk"),
                sampleProduct("Lemon Almond")));
        model.put("discountCode", "SAMPLE10");
        model.put("discountExpiry", "31 December 2026");
        model.put("quote", "A little moment of joy with my afternoon tea.");
        model.put("quoteAuthor", "Sample customer");
        model.put("actionUrl", frontendBase
                + (template.equals("password-reset") ? "/reset-password?token=preview" : "/products"));
        model.put("actionLabel", switch (template) {
            case "password-reset" -> "Reset your password";
            case "order-shipped" -> "Track your parcel";
            default -> "View your order";
        });
        if (template.equals("claim-orders")) {
            model.put("actionUrl", frontendBase + "/account/claim?token=preview");
            model.put("actionLabel", "Add my earlier orders");
        }
        return model;
    }

    public void require(String template) {
        if (!NAMES.contains(template)) {
            throw new NotFoundException("Email template not found");
        }
    }

    public static String plainText(String html) {
        String text = html
                .replaceAll("(?is)<(head|style|script)[^>]*>.*?</\\1>", "")
                .replaceAll("(?is)<!--.*?-->", "")
                .replaceAll("(?i)<br\\s*/?>|</(?:p|h[1-6]|tr|table|div)>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("[ \t]+", " ")
                .replaceAll("\n\\s*\n+", "\n\n");
        return HtmlUtils.htmlUnescape(text).trim();
    }

    private Map<String, Object> flowSample(String template) {
        for (EmailFlow flow : EmailFlow.values()) {
            for (int step = 0; step < 5; step++) {
                if (FlowCatalog.previewName(flow, step).equals(template)) {
                    FlowData data = new FlowData(
                            List.of(sampleProduct("Double Chocolate Chunk"), sampleProduct("Lemon Almond")),
                            List.of(Map.of("name", "Double Chocolate Chunk", "variant", "Box of 6", "quantity", 1,
                                    "lineTotal", "R 175.00", "imageUrl", publicBase + "/email/cookie.png")),
                            List.of(Map.of("quote", "A little moment of joy with my afternoon tea.", "author", "Sample customer",
                                    "stars", "★★★★★", "rating", 5)),
                            List.of("Courier to your door: R 80.00 (2–4 business days).", "Collect from our bakery in Strandfontein for free."),
                            "SAMPLE10", "31 December 2026", 10, "Double Chocolate Chunk", frontendBase + "/cart", 21);
                    FlowCopy.Message message = FlowCopy.compose(flow, step, data, frontendBase, UnaryOperator.identity());
                    Map<String, Object> model = new HashMap<>(message.model());
                    model.put("subject", message.subject());
                    model.put("marketing", true);
                    model.put("unsubscribeUrl", frontendBase + "/email/unsubscribe?e=sam%40example.com&t=preview");
                    return model;
                }
            }
        }
        throw new NotFoundException("Email template not found");
    }

    private Map<String, Object> sampleProduct(String name) {
        return Map.of(
                "name", name,
                "price", "R 30.00",
                "imageUrl", publicBase + "/email/cookie.png",
                "url", frontendBase + "/products");
    }

    // Outlook ignores border-radius on links, so it gets a VML rounded button instead.
    private static String outlookButton(String url, String label) {
        return "<!--[if mso]><v:roundrect xmlns:v=\"urn:schemas-microsoft-com:vml\" "
                + "xmlns:w=\"urn:schemas-microsoft-com:office:word\" href=\"" + HtmlUtils.htmlEscape(url) + "\" "
                + "style=\"height:48px;v-text-anchor:middle;width:260px;\" arcsize=\"50%\" stroke=\"f\" fillcolor=\"#C59F71\">"
                + "<w:anchorlock/><center style=\"color:#2E0E0B;font-family:Arial,sans-serif;font-size:16px;font-weight:bold;\">"
                + HtmlUtils.htmlEscape(label) + "</center></v:roundrect><![endif]-->";
    }
}
