package za.ac.cput.guiltfreecookie.service.order;

// Published inside the order's transaction; emails go out only once it commits.
public final class OrderEvents {

    private OrderEvents() {
    }

    public record Paid(String orderNumber) {
    }

    public record Shipped(String orderNumber) {
    }

    public record ReadyForPickup(String orderNumber) {
    }

    public record Delivered(String orderNumber) {
    }
}
