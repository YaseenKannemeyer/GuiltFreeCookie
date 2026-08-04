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

        Cookie existing = cookieRepository.findById(cookie.getCookieId()).orElse(null);

        if (existing == null) {
            return null;
        }

        Cookie updated = new Cookie.Builder()
                .copy(existing)
                .setCategory(cookie.getCategory())
                .setDescription(cookie.getDescription())
                .setIngredients(cookie.getIngredients())
                .setAllergies(cookie.getAllergies())
                .setPrice(cookie.getPrice())
                .build();

        return cookieRepository.save(updated);
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

}