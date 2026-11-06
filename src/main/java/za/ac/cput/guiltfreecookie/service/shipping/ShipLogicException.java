package za.ac.cput.guiltfreecookie.service.shipping;

// A booking or waybill problem worth showing to the admin as it is.
public class ShipLogicException extends RuntimeException {

    public ShipLogicException(String message) {
        super(message);
    }
}
