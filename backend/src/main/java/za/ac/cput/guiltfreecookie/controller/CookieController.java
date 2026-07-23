package za.ac.cput.guiltfreecookie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.service.CookieService;

import java.util.List;

@RestController
@RequestMapping("/cookie")
public class CookieController {

    private final CookieService cookieService;

    @Autowired
    public CookieController(CookieService cookieService) {
        this.cookieService = cookieService;
    }

    // TEST ENDPOINT
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Cookie API is working!");
    }

    // Get all - root path
    @GetMapping
    public ResponseEntity<List<Cookie>> root() {
        return ResponseEntity.ok(cookieService.getAll());
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<Cookie>> getAll() {
        return ResponseEntity.ok(cookieService.getAll());
    }

    @PostMapping("/create")
    public ResponseEntity<Cookie> create(@RequestBody Cookie cookie) {
        Cookie created = cookieService.create(cookie);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/read/{cookieId}")
    public ResponseEntity<Cookie> read(@PathVariable String cookieId) {
        Cookie cookie = cookieService.read(cookieId);
        if (cookie == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(cookie);
    }

    @PutMapping("/update")
    public ResponseEntity<Cookie> update(@RequestBody Cookie cookie) {
        return ResponseEntity.ok(cookieService.update(cookie));
    }

    @DeleteMapping("/delete/{cookieId}")
    public ResponseEntity<Void> delete(@PathVariable String cookieId) {
        cookieService.delete(cookieId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Cookie>> getByCategory(@PathVariable CookieCategory category) {
        return ResponseEntity.ok(cookieService.getByCategory(category));
    }

    @GetMapping("/price-range")
    public ResponseEntity<List<Cookie>> getByPriceRange(
            @RequestParam Double min,
            @RequestParam Double max) {
        return ResponseEntity.ok(cookieService.getByPriceRange(min, max));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Cookie>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(cookieService.searchAll(keyword));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Cookie>> filterCookies(
            @RequestParam(required = false) CookieCategory category,
            @RequestParam(required = false, defaultValue = "0") Double minPrice,
            @RequestParam(required = false, defaultValue = "9999") Double maxPrice) {
        return ResponseEntity.ok(cookieService.filterCookies(category, minPrice, maxPrice));
    }

    @GetMapping("/allergy-free")
    public ResponseEntity<List<Cookie>> getAllergyFree(@RequestParam String allergy) {
        return ResponseEntity.ok(cookieService.getAllergyFree(allergy));
    }
}