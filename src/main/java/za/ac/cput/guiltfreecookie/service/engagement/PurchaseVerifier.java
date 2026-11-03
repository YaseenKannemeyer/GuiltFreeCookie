package za.ac.cput.guiltfreecookie.service.engagement;

// Decides the "verified buyer" badge when a review is written.
public interface PurchaseVerifier {

    boolean hasBought(String email, String cookieId);
}
