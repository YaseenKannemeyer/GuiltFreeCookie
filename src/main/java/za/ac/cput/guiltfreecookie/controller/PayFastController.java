package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import za.ac.cput.guiltfreecookie.service.payment.PayFastNotificationService;

import java.util.LinkedHashMap;
import java.util.Map;

// Called by PayFast, not the frontend.
@Hidden
@RestController
public class PayFastController {

    private final PayFastNotificationService notificationService;

    public PayFastController(PayFastNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // The signature covers the fields in the order PayFast sent them, so read them in that order.
    @PostMapping("/api/v1/payments/payfast/notify")
    public ResponseEntity<Void> notify(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((name, values) -> params.put(name, values.length > 0 ? values[0] : ""));
        return notificationService.handle(params, request.getRemoteAddr())
                ? ResponseEntity.ok().build()
                : ResponseEntity.badRequest().build();
    }
}
