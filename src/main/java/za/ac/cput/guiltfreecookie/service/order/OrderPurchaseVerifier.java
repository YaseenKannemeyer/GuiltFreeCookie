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
    public boolean hasBought(String email, String cookieId) {
        return itemRepository.hasBought(email, cookieId, OrderStatus.PAID_STATUSES);
    }
}
