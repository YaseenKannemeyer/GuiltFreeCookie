package za.ac.cput.guiltfreecookie.service.engagement;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.dto.engagement.RatingSummary;
import za.ac.cput.guiltfreecookie.dto.engagement.SiteInfo;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;

// Shop identity from app.site.* plus live stats from approved reviews and paid orders. Email templates call setting() directly.
@Service
public class SiteInfoService {

    private final Environment environment;
    private final ReviewService reviewService;
    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public SiteInfoService(Environment environment, ReviewService reviewService,
                           CustomerOrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.environment = environment;
        this.reviewService = reviewService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public String setting(String name) {
        return environment.getProperty("app.site." + name, "");
    }

    public SiteInfo info() {
        RatingSummary rating = reviewService.overall();
        return new SiteInfo(
                setting("support-email"),
                setting("phone"),
                setting("whatsapp"),
                setting("hours"),
                setting("response-time"),
                setting("address"),
                new SiteInfo.Social(setting("instagram"), setting("facebook"), setting("tiktok")),
                new SiteInfo.Stats(rating.averageRating(), rating.reviewCount(),
                        orderRepository.countCustomers(OrderStatus.PAID_STATUSES),
                        orderItemRepository.countCookies(OrderStatus.PAID_STATUSES)));
    }
}
