package za.ac.cput.guiltfreecookie.service.notification;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import za.ac.cput.guiltfreecookie.exception.*;
import za.ac.cput.guiltfreecookie.service.engagement.SiteInfoService;

@Service
public class EmailTemplates {

    public static final List<String> NAMES = List.of(
        "password-reset",
        "contact-auto-reply",
        "contact-to-shop",
        "order-confirmation",
        "order-shipped",
        "order-ready-for-pickup"
    );
    private final SpringTemplateEngine engine;
    private final SiteInfoService site;
    private final String publicBase, frontendBase;

    public EmailTemplates(
        SpringTemplateEngine engine,
        SiteInfoService site,
        @Value(
            "${app.public-base-url:http://localhost:8080/guiltFreeCookie}"
        ) String publicBase,
        @Value("${app.frontend.base-url}") String frontendBase
    ) {
        this.engine = engine;
        this.site = site;
        this.publicBase = publicBase.replaceAll("/+$", "");
        this.frontendBase = frontendBase.replaceAll("/+$", "");
    }

    public String render(String template, Map<String, Object> model) {
        require(template);
        Map<String, Object> values = new HashMap<>();
        values.put("site", site);
        values.put("publicBase", publicBase);
        values.put("frontendBase", frontendBase);
        values.put("heroImage", publicBase + "/email/cookie.png");
        values.put("unsubscribeUrl", "");
        values.put("marketing", false);
        values.putAll(model);
        String url = Objects.toString(
            values.get("actionUrl"),
            frontendBase + "/products"
        );
        if (
            !url.startsWith("https://") && !url.startsWith("http://")
        ) throw new BadRequestException(
            "Email actionUrl must be an absolute HTTP URL"
        );
        String label = Objects.toString(
            values.get("actionLabel"),
            "Visit the bakery"
        );
        values.put("actionUrl", url);
        values.put("actionLabel", label);
        values.put(
            "ctaVml",
            "<!--[if mso]><v:roundrect xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:w=\"urn:schemas-microsoft-com:office:word\" href=\"" +
                HtmlUtils.htmlEscape(url) +
                "\" style=\"height:48px;v-text-anchor:middle;width:260px;\" arcsize=\"50%\" stroke=\"f\" fillcolor=\"#C59F71\"><w:anchorlock/><center style=\"color:#2E0E0B;font-family:Arial,sans-serif;font-size:16px;font-weight:bold;\">" +
                HtmlUtils.htmlEscape(label) +
                "</center></v:roundrect><![endif]-->"
        );
        Context context = new Context(Locale.ENGLISH, values);
        return engine.process("email/" + template, context);
    }

    public Map<String, Object> sample(String template) {
        require(template);
        Map<String, Object> model = new HashMap<>();
        model.put("name", "Sam");
        model.put("email", "sam@example.com");
        model.put("phone", "082 000 0000");
        model.put("subject", "A question about your cookies");
        model.put(
            "message",
            "Could you tell me more about the ingredients in your chocolate cookies?"
        );
        model.put("messageId", "preview-message");
        model.put("resetMinutes", 60);
        model.put("orderNumber", "GFC-PREVIEW");
        model.put("courierName", "Example courier");
        model.put("trackingNumber", "PREVIEW-123");
        model.put(
            "deliveryDetails",
            "Example delivery: 12 Bakery Lane, Cape Town, 8001"
        );
        if (template.equals("order-ready-for-pickup")) {
            model.put(
                "pickupDetails",
                "Example pickup: please bring your order number. We will confirm the collection address in your order."
            );
            model.put("deliveryDetails", null);
        }
        model.put("subtotal", "R 180.00");
        model.put("discount", "- R 18.00");
        model.put("shipping", "R 60.00");
        model.put("vat", "R 28.96");
        model.put("total", "R 222.00");
        model.put(
            "items",
            List.of(
                Map.of(
                    "name",
                    "Double Chocolate Chunk",
                    "variant",
                    "Box of 6",
                    "quantity",
                    1,
                    "lineTotal",
                    "R 180.00"
                )
            )
        );
        model.put(
            "products",
            List.of(
                Map.of(
                    "name",
                    "Double Chocolate Chunk",
                    "price",
                    "R 30.00",
                    "imageUrl",
                    publicBase + "/email/cookie.png",
                    "url",
                    frontendBase + "/products"
                ),
                Map.of(
                    "name",
                    "Lemon Almond",
                    "price",
                    "R 30.00",
                    "imageUrl",
                    publicBase + "/email/cookie.png",
                    "url",
                    frontendBase + "/products"
                )
            )
        );
        model.put("discountCode", "SAMPLE10");
        model.put("discountExpiry", "31 December 2026");
        model.put("quote", "A little moment of joy with my afternoon tea.");
        model.put("quoteAuthor", "Sample customer");
        model.put(
            "actionUrl",
            frontendBase +
                (template.equals("password-reset")
                    ? "/reset-password?token=preview"
                    : "/products")
        );
        model.put(
            "actionLabel",
            template.equals("password-reset")
                ? "Reset your password"
                : template.equals("order-shipped")
                    ? "Track your parcel"
                    : "View your order"
        );
        return model;
    }

    public void require(String template) {
        if (!NAMES.contains(template)) throw new NotFoundException(
            "Email template not found"
        );
    }

    public static String plainText(String html) {
        return HtmlUtils.htmlUnescape(
            html
                .replaceAll("(?is)<(head|style|script)[^>]*>.*?</\\1>", "")
                .replaceAll("(?is)<!--.*?-->", "")
                .replaceAll("(?i)<br\\s*/?>|</(?:p|h[1-6]|tr|table|div)>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("[ \t]+", " ")
                .replaceAll("\n\s*\n+", "\n\n")
        ).trim();
    }
}
