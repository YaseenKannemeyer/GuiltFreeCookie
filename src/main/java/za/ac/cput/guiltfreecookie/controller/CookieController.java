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
@CrossOrigin(origins = "http://localhost:5173")
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


    @CrossOrigin
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

@PutMapping("/update/{cookieId}")
public ResponseEntity<Cookie> update(
        @PathVariable String cookieId,
        @RequestBody Cookie cookie) {



    Cookie updated = cookieService.update(cookie);

    if (updated == null) {
        return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(updated);
}

    @DeleteMapping("/delete/{cookieId}")
    public ResponseEntity<Void> delete(@PathVariable String cookieId) {
        cookieService.delete(cookieId);
        return ResponseEntity.noContent().build();
    }

}