package za.ac.cput.guiltfreecookie.service.engagement;

import org.springframework.stereotype.Component;

@Component
public class NoPurchaseVerifier implements PurchaseVerifier {

    public boolean hasBought(String email, String cookieId) {
        return false;
    }
}
