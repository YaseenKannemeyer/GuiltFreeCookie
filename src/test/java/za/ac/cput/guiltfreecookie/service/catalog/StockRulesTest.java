package za.ac.cput.guiltfreecookie.service.catalog;

import org.junit.jupiter.api.Test;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.domain.StockStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StockRulesTest {

    private final StockRules rules = new StockRules(12);
    private final List<CookieVariant> singleAndBox = List.of(
            new CookieVariant.Builder().setUnitsPerPack(1).build(),
            new CookieVariant.Builder().setUnitsPerPack(6).build());

    private Cookie stock(Integer stock) {
        return new Cookie.Builder().setStockQuantity(stock).build();
    }

    @Test
    void statusFollowsStockAndThreshold() {
        assertEquals(StockStatus.IN_STOCK, rules.status(stock(13), singleAndBox));
        assertEquals(StockStatus.LOW_STOCK, rules.status(stock(12), singleAndBox));
        assertEquals(StockStatus.LOW_STOCK, rules.status(stock(1), singleAndBox));
        assertEquals(StockStatus.OUT_OF_STOCK, rules.status(stock(0), singleAndBox));
        assertEquals(StockStatus.OUT_OF_STOCK, rules.status(stock(null), singleAndBox));
    }

    @Test
    void aCookieWithOnlyBoxesIsOutWhenAFullBoxCantBeFilled() {
        List<CookieVariant> boxesOnly = List.of(new CookieVariant.Builder().setUnitsPerPack(6).build());

        assertEquals(StockStatus.OUT_OF_STOCK, rules.status(stock(5), boxesOnly));
        assertEquals(StockStatus.LOW_STOCK, rules.status(stock(6), boxesOnly));
    }

    @Test
    void aCookieWithNoActivePacksCantBeBought() {
        assertEquals(StockStatus.OUT_OF_STOCK, rules.status(stock(100), List.of()));
    }

    @Test
    void aCookiesOwnThresholdWins() {
        Cookie cookie = new Cookie.Builder().setStockQuantity(20).setLowStockThreshold(24).build();

        assertEquals(StockStatus.LOW_STOCK, rules.status(cookie, singleAndBox));
    }
}
