package za.ac.cput.guiltfreecookie.dto.catalog;

import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieImage;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.domain.StockStatus;

import java.util.List;

public record AdminCookieView(
        Cookie cookie,
        StockStatus stockStatus,
        List<CookieVariant> variants,
        List<CookieImage> images) {
}
