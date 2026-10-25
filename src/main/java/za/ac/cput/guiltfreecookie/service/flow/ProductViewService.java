package za.ac.cput.guiltfreecookie.service.flow;

import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.domain.Cart;
import za.ac.cput.guiltfreecookie.domain.CartStatus;
import za.ac.cput.guiltfreecookie.dto.flow.ProductViewEvent;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.util.Helper;

// Starts browse abandonment for shoppers we can already email. Anonymous views are ignored.
@Service
public class ProductViewService {

    private final CartRepository cartRepository;
    private final CookieRepository cookieRepository;
    private final FlowEnrollmentService enrollments;

    public ProductViewService(CartRepository cartRepository, CookieRepository cookieRepository,
                              FlowEnrollmentService enrollments) {
        this.cartRepository = cartRepository;
        this.cookieRepository = cookieRepository;
        this.enrollments = enrollments;
    }

    public void viewed(ProductViewEvent event, String signedInEmail) {
        if (event == null || Helper.isNullOrEmpty(event.cookieId())) {
            return;
        }
        String email = signedInEmail;
        if (email == null && !Helper.isNullOrEmpty(event.cartId())) {
            email = cartRepository.findById(event.cartId())
                    .filter(cart -> cart.getStatus() == CartStatus.ACTIVE)
                    .map(Cart::getCustomerEmail).orElse(null);
        }
        if (email != null && cookieRepository.findById(event.cookieId()).filter(cookie -> !cookie.isArchived()).isPresent()) {
            enrollments.browse(email, event.cookieId());
        }
    }
}
