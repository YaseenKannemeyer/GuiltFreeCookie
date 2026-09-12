package za.ac.cput.guiltfreecookie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.service.CookieService;
import za.ac.cput.guiltfreecookie.service.ImageStorageService;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.util.List;

@RestController
@RequestMapping("/cookie")
public class CookieController {

    private final CookieService cookieService;
    private final ImageStorageService imageStorageService;

    @Autowired
    public CookieController(CookieService cookieService, ImageStorageService imageStorageService) {
        this.cookieService = cookieService;
        this.imageStorageService = imageStorageService;
    }

    // TEST ENDPOINT
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Cookie API is working!");
    }


    @GetMapping("/getAll")
    public ResponseEntity<List<Cookie>> getAll() {
        return ResponseEntity.ok(cookieService.getAllActive());
    }

    @GetMapping("/getAllForAdmin")
    public ResponseEntity<List<Cookie>> getAllForAdmin() {
        return ResponseEntity.ok(cookieService.getAll());
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Cookie> create(
            @RequestPart("cookie") Cookie cookie,
            @RequestPart("image") MultipartFile image) {

        if (!isValidCookie(cookie)) {
            return ResponseEntity.badRequest().build();
        }

        String imagePath;
        try {
            imagePath = imageStorageService.store(image);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        Cookie toCreate = new Cookie.Builder().copy(cookie).setImage(imagePath).build();
        Cookie created = cookieService.create(toCreate);
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

@PutMapping(value = "/update/{cookieId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<Cookie> update(
        @PathVariable String cookieId,
        @RequestPart("cookie") Cookie cookie,
        @RequestPart(value = "image", required = false) MultipartFile image) {

    Cookie existing = cookieService.read(cookieId);
    if (existing == null) {
        return ResponseEntity.notFound().build();
    }

    if (!isValidCookie(cookie)) {
        return ResponseEntity.badRequest().build();
    }

    String imagePath = existing.getImage();
    if (image != null && !image.isEmpty()) {
        try {
            imagePath = imageStorageService.store(image);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        imageStorageService.delete(existing.getImage());
    }

    Cookie toUpdate = new Cookie.Builder().copy(cookie).setCookieId(cookieId).setImage(imagePath).build();
    Cookie updated = cookieService.update(toUpdate);

    if (updated == null) {
        return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(updated);
}

    @DeleteMapping("/delete/{cookieId}")
    public ResponseEntity<Void> delete(@PathVariable String cookieId) {
        Cookie existing = cookieService.read(cookieId);
        cookieService.delete(cookieId);
        if (existing != null) {
            imageStorageService.delete(existing.getImage());
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/archive/{cookieId}")
    public ResponseEntity<Cookie> archive(@PathVariable String cookieId) {
        Cookie updated = cookieService.setArchived(cookieId, true);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/unarchive/{cookieId}")
    public ResponseEntity<Cookie> unarchive(@PathVariable String cookieId) {
        Cookie updated = cookieService.setArchived(cookieId, false);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    private boolean isValidCookie(Cookie cookie) {
        return !Helper.isNullOrEmpty(cookie.getDescription())
                && !Helper.isNullOrEmpty(cookie.getIngredients())
                && !Helper.isNullOrEmpty(cookie.getAllergies())
                && cookie.getPrice() != null
                && cookie.getPrice() > 0;
    }

}