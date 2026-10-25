package za.ac.cput.guiltfreecookie.service.order;

import org.springframework.stereotype.Component;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;
import za.ac.cput.guiltfreecookie.service.engagement.PurchaseVerifier;

// A "verified buyer" has a paid, unrefunded order containing that cookie.
@Component
public class OrderPurchaseVerifier implements PurchaseVerifier {

    private final OrderItemRepository itemRepository;

    public OrderPurchaseVerifier(OrderItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    // Account orders only: there's no email check at sign-up, so a guest order with the same email doesn't count
    // until its owner claims it from their inbox.
    public boolean hasBought(String email, String cookieId) {
        return itemRepository.hasBought(email, cookieId, OrderStatus.PAID_STATUSES);
    }
}
