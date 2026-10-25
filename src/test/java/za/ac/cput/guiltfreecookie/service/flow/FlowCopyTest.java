package za.ac.cput.guiltfreecookie.service.flow;

import org.junit.jupiter.api.Test;
import za.ac.cput.guiltfreecookie.domain.EmailFlow;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowCopyTest {

    private static final String SHOP = "https://shop.example";
    private static final List<Map<String, Object>> PRODUCTS = List.of(
            Map.of("name", "Lemon Almond", "price", "R 30.00", "imageUrl", "https://img", "url", SHOP + "/product/lemon"));

    @Test
    void everyStepHasASubjectHeadlineAndAButtonAndNoHealthClaims() {
        FlowData data = data("WELCOME-ABC234", List.of());
        for (EmailFlow flow : EmailFlow.values()) {
            for (int step = 0; step < FlowCatalog.stepCount(flow); step++) {
                FlowCopy.Message message = FlowCopy.compose(flow, step, data, SHOP, UnaryOperator.identity());
                String words = (message.subject() + " " + message.model().get("headline") + " "
                        + message.model().get("paragraphs") + " " + message.model().get("bullets")).toLowerCase(Locale.ROOT);

                assertFalse(message.subject().isBlank(), flow + " " + step);
                assertNotNull(message.model().get("actionLabel"), flow + " " + step);
                for (String claim : List.of("diabet", "healthy", "blood sugar", "cures", "sugar-free", "weight")) {
                    assertFalse(words.contains(claim), flow + " " + step + " mentions " + claim);
                }
            }
        }
    }

    @Test
    void reviewsOnlyAppearWhenRealOnesExist() {
        FlowCopy.Message none = FlowCopy.compose(EmailFlow.WELCOME, 3, data(null, List.of()), SHOP, UnaryOperator.identity());
        FlowCopy.Message some = FlowCopy.compose(EmailFlow.WELCOME, 3,
                data(null, List.of(Map.of("quote", "Lovely", "author", "Ayesha", "stars", "★★★★★", "rating", 5))),
                SHOP, UnaryOperator.identity());

        assertEquals(List.of(), none.model().get("reviews"));
        assertFalse(String.valueOf(none.model().get("paragraphs")).contains("customers have said"));
        assertEquals(1, ((List<?>) some.model().get("reviews")).size());
    }

    @Test
    void noCodeMeansNoPromiseOfOne() {
        FlowCopy.Message welcome = FlowCopy.compose(EmailFlow.WELCOME, 0, data(null, List.of()), SHOP, UnaryOperator.identity());

        assertEquals("Welcome to GuiltFreeCookie", welcome.subject());
        assertNull(welcome.model().get("discountCode"));
        assertFalse(String.valueOf(welcome.model().get("paragraphs")).contains("%"));
    }

    @Test
    void everyLinkGoesThroughTracking() {
        FlowCopy.Message cart = FlowCopy.compose(EmailFlow.BROWSE_ABANDONMENT, 0, data(null, List.of()), SHOP,
                url -> "tracked:" + url);

        assertEquals("tracked:" + SHOP + "/cart", cart.model().get("actionUrl"));
        List<?> products = (List<?>) cart.model().get("products");
        assertTrue(((Map<?, ?>) products.getFirst()).get("url").toString().startsWith("tracked:"));
    }

    private static FlowData data(String code, List<Map<String, Object>> reviews) {
        return new FlowData(PRODUCTS, List.of(Map.of("name", "Lemon Almond", "variant", "Single", "quantity", 1,
                "lineTotal", "R 30.00", "imageUrl", "https://img")), reviews, List.of("Collect for free."),
                code, code == null ? null : "31 October 2026", 10, "Lemon Almond", SHOP + "/cart", 21);
    }
}
