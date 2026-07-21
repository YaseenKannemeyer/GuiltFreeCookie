package za.ac.cput.guiltfreecookie.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;

import java.util.List;

@Service
public class CookieService implements IService<Cookie, String> {

    private final CookieRepository cookieRepository;

    @Autowired
    public CookieService(CookieRepository cookieRepository) {
        this.cookieRepository = cookieRepository;
    }

    @Override
    public Cookie create(Cookie cookie) {
        return this.cookieRepository.save(cookie);
    }

    @Override
    public Cookie read(String id) {
        return this.cookieRepository.findById(id).orElse(null);
    }

    @Override
    public Cookie update(Cookie cookie) {
        return this.cookieRepository.save(cookie);
    }

    @Override
    public boolean delete(String id) {
        if (this.cookieRepository.existsById(id)) {
            this.cookieRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public List<Cookie> getAll() {
        return this.cookieRepository.findAll();
    }

    // Custom methods
    public List<Cookie> getByCategory(CookieCategory category) {
        return this.cookieRepository.findByCategory(category);
    }

    public List<Cookie> getByPriceRange(Double minPrice, Double maxPrice) {
        return this.cookieRepository.findByPriceBetween(minPrice, maxPrice);
    }

    public List<Cookie> getByMaxPrice(Double price) {
        return this.cookieRepository.findByPriceLessThanEqual(price);
    }

    public List<Cookie> getByMinPrice(Double price) {
        return this.cookieRepository.findByPriceGreaterThanEqual(price);
    }

    public List<Cookie> searchByDescription(String keyword) {
        return this.cookieRepository.findByDescriptionContainingIgnoreCase(keyword);
    }

    public List<Cookie> searchByIngredient(String ingredient) {
        return this.cookieRepository.findByIngredientsContainingIgnoreCase(ingredient);
    }

    public List<Cookie> searchByAllergy(String allergy) {
        return this.cookieRepository.findByAllergiesContainingIgnoreCase(allergy);
    }

    public List<Cookie> getAllergyFree(String allergy) {
        return this.cookieRepository.findByAllergiesNotContainingIgnoreCase(allergy);
    }

    public List<Cookie> searchAll(String keyword) {
        return this.cookieRepository.searchCookies(keyword);
    }

    public List<Cookie> filterCookies(CookieCategory category, Double minPrice, Double maxPrice) {
        return this.cookieRepository.filterCookies(category, minPrice, maxPrice);
    }

    public Long countByCategory(CookieCategory category) {
        return this.cookieRepository.countByCategory(category);
    }

    public boolean cookieExists(String cookieId) {
        return this.cookieRepository.existsByCookieId(cookieId);
    }
}