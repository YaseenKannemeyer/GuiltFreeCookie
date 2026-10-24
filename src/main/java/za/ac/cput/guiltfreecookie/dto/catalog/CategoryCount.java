package za.ac.cput.guiltfreecookie.dto.catalog;

import za.ac.cput.guiltfreecookie.domain.CookieCategory;

public record CategoryCount(CookieCategory category, String label, long count) {
}
