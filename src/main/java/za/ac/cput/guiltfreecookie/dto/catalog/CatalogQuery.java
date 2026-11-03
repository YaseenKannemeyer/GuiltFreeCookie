package za.ac.cput.guiltfreecookie.dto.catalog;

import za.ac.cput.guiltfreecookie.domain.CookieCategory;

public record CatalogQuery(
        String q,
        CookieCategory category,
        Double minPrice,
        Double maxPrice,
        Boolean inStock,
        Boolean featured,
        String sort,
        Integer page,
        Integer size) {
}
