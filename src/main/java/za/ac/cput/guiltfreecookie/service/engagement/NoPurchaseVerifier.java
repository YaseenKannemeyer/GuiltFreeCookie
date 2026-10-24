package za.ac.cput.guiltfreecookie.service.engagement;

import org.springframework.stereotype.Component;

// Until orders exist nobody is a verified buyer. Phase 4 replaces this bean.
@Component
public class NoPurchaseVerifier implements PurchaseVerifier {

    @Override
    public boolean hasBought(String email, String cookieId) {
        return false;
    }
}
