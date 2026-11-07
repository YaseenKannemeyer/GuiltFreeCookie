package za.ac.cput.guiltfreecookie.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import za.ac.cput.guiltfreecookie.util.ShopMetrics;

@Component
public class ShopMetricsStartup {

    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        ShopMetrics.registerAll();
    }
}
