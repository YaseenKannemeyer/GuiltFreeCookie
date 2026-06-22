package za.ac.cput.guiltfreecookie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.service.CookieService;

import java.util.List;

@RestController
@RequestMapping("/cookie")
public class CookieController {

    private CookieService cookieService;

    @Autowired
    public CookieController(CookieService cookieService) {
        this.cookieService = cookieService;
    }

    @PostMapping("/create")
    public Cookie create(@RequestBody Cookie cookie) {
        return cookieService.create(cookie);
    }

    @GetMapping("/read/{cookieId}")
    public Cookie read(@PathVariable String cookieId) {
        return cookieService.read(cookieId);
    }

    @PutMapping("/update")
    public Cookie update(@RequestBody Cookie cookie) {
        return cookieService.update(cookie);
    }

    @DeleteMapping("/delete/{cookieId}")
    public boolean delete(@PathVariable String cookieId) {
        return cookieService.delete(cookieId);
    }

    @GetMapping("/getAll")
    public List<Cookie> getAll() {
        return cookieService.getAll();
    }
}
