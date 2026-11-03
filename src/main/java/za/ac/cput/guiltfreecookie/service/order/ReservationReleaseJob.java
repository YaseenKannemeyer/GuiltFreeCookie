package za.ac.cput.guiltfreecookie.service.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationReleaseJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationReleaseJob.class);

    private final OrderService orderService;

    public ReservationReleaseJob(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${app.order.expiry-check-ms}", initialDelayString = "${app.order.expiry-check-ms}")
    public void releaseExpiredReservations() {
        int released = orderService.releaseExpired();
        if (released > 0) {
            log.info("Cancelled {} unpaid order(s) and returned their stock", released);
        }
    }
}
