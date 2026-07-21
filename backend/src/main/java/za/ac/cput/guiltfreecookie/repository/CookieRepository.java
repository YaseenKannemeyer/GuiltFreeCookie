package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;

import java.util.List;

@Repository
public interface CookieRepository extends JpaRepository<Cookie, String> {

    // Find all cookies by category
    List<Cookie> findByCategory(CookieCategory category);

    // Find cookies by price range
    List<Cookie> findByPriceBetween(Double minPrice, Double maxPrice);

    // Find cookies cheaper than a price
    List<Cookie> findByPriceLessThanEqual(Double price);

    // Find cookies more expensive than a price
    List<Cookie> findByPriceGreaterThanEqual(Double price);

    // Search by description (partial match, case insensitive)
    List<Cookie> findByDescriptionContainingIgnoreCase(String keyword);

    // Search by ingredients (partial match, case insensitive)
    List<Cookie> findByIngredientsContainingIgnoreCase(String ingredient);

    // Search by allergies info
    List<Cookie> findByAllergiesContainingIgnoreCase(String allergy);

    // Find cookies that don't contain a specific allergy
    List<Cookie> findByAllergiesNotContainingIgnoreCase(String allergy);

    // Custom query: Search across multiple fields
    @Query("SELECT c FROM Cookie c WHERE " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.ingredients) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.allergies) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Cookie> searchCookies(@Param("keyword") String keyword);

    // Custom query: Filter by category and price range
    @Query("SELECT c FROM Cookie c WHERE " +
            "(:category IS NULL OR c.category = :category) AND " +
            "c.price BETWEEN :minPrice AND :maxPrice")
    List<Cookie> filterCookies(
            @Param("category") CookieCategory category,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice);

    // Count cookies by category
    Long countByCategory(CookieCategory category);

    // Check if cookie ID exists
    Boolean existsByCookieId(String cookieId);
}