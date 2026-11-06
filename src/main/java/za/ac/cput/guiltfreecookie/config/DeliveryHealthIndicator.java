package za.ac.cput.guiltfreecookie.config;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;
import za.ac.cput.guiltfreecookie.service.shipping.ShipLogicClient;

// The courier API answers and accepts our key. Up while courier integration is switched off.
@Component("deliveryHealthIndicator")
public class DeliveryHealthIndicator extends AbstractHealthIndicator {

    private final ShipLogicClient shipLogic;

    public DeliveryHealthIndicator(ShipLogicClient shipLogic) {
        this.shipLogic = shipLogic;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        if (!shipLogic.enabled() || shipLogic.reachable()) {
            builder.up();
        } else {
            builder.down();
        }
    }
}
