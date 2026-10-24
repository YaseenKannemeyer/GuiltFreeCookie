package za.ac.cput.guiltfreecookie.service.engagement;

public interface PurchaseVerifier {
    boolean hasBought(String email, String cookieId);
}
