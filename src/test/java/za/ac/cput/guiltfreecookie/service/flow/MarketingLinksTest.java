package za.ac.cput.guiltfreecookie.service.flow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketingLinksTest {

    private final MarketingLinks links = new MarketingLinks("secret", "https://api.example.co.za/guiltFreeCookie",
            "https://shop.example.co.za");

    @Test
    void tokensMatchTheirOwnAddressOnly() {
        String token = links.token("thandi@example.co.za");

        assertTrue(links.matches("thandi@example.co.za", token));
        assertTrue(links.matches("Thandi@Example.co.za", token));
        assertFalse(links.matches("sipho@example.co.za", token));
        assertFalse(links.matches("thandi@example.co.za", token + "x"));
        assertFalse(links.matches("thandi@example.co.za", null));
        assertFalse(new MarketingLinks("other", "https://a", "https://b").matches("thandi@example.co.za", token));
    }

    @Test
    void trackedLinksOnlyEverLeadToTheStorefront() {
        assertTrue(links.isStorefront("https://shop.example.co.za/cart"));
        assertTrue(links.isStorefront("https://shop.example.co.za/product/lemon?utm=1"));
        assertFalse(links.isStorefront("https://evil.example.com/shop.example.co.za"));
        assertFalse(links.isStorefront("https://shop.example.co.za@evil.example.com/"));
        assertFalse(links.isStorefront("https://shop.example.co.za.evil.com/"));
        assertFalse(links.isStorefront("http://shop.example.co.za/cart"));
        assertFalse(links.isStorefront("https://shop.example.co.za:8443/cart"));
        assertFalse(links.isStorefront("//evil.example.com"));
        assertFalse(links.isStorefront("javascript:alert(1)"));
        assertFalse(links.isStorefront("not a url"));
        assertFalse(links.isStorefront(null));
    }

    @Test
    void unsubscribeLinksCarryTheEncodedAddressAndToken() {
        String page = links.unsubscribePage("a+b@example.co.za");

        assertTrue(page.startsWith("https://shop.example.co.za/email/unsubscribe?e=a%2Bb%40example.co.za&t="));
        assertTrue(links.oneClickUnsubscribe("a@example.co.za")
                .startsWith("https://api.example.co.za/guiltFreeCookie/api/v1/email/unsubscribe/one-click?e="));
    }
}
