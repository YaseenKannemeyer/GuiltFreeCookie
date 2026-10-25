package za.ac.cput.guiltfreecookie.service.flow;

import za.ac.cput.guiltfreecookie.domain.EmailFlow;
import za.ac.cput.guiltfreecookie.domain.EmailFlowSettings;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

// Each flow's steps, timed from the moment of enrolment. Copy for each step is in FlowMessages.
public final class FlowCatalog {

    // Sunset waits this long after its last email before pausing someone who didn't click.
    public static final Duration SUNSET_GRACE = Duration.ofHours(48);

    private FlowCatalog() {
    }

    public static List<Duration> delays(EmailFlow flow, EmailFlowSettings settings) {
        return switch (flow) {
            // The last one says "your code ends tomorrow", so it follows the code's lifetime (5 days → 96 hours).
            case WELCOME -> List.of(Duration.ZERO, hours(24), hours(48), hours(72),
                    hours(Math.max(72, settings.getWelcomeCodeDays() * 24L - 24)));
            case BROWSE_ABANDONMENT -> List.of(hours(1), hours(24));
            // Enrolled once the cart has sat for an hour, so the first reminder goes straight away.
            case CART_ABANDONMENT -> List.of(Duration.ZERO, hours(23), hours(47));
            // Timed from delivery.
            case POST_PURCHASE -> List.of(Duration.ofDays(3), Duration.ofDays(7));
            case REPLENISHMENT -> List.of(Duration.ofDays(settings.getReplenishmentDays()));
            case WINBACK -> List.of(Duration.ZERO, Duration.ofDays(3), Duration.ofDays(7));
            case SUNSET -> List.of(Duration.ZERO, hours(48), hours(72));
        };
    }

    // Preview names for the admin email list, e.g. "flow-welcome-1".
    public static List<String> previewNames() {
        return Arrays.stream(EmailFlow.values())
                .flatMap(flow -> IntStream.rangeClosed(1, stepCount(flow))
                        .mapToObj(step -> previewName(flow, step - 1)))
                .toList();
    }

    public static String previewName(EmailFlow flow, int step) {
        return "flow-" + flow.name().toLowerCase().replace('_', '-') + "-" + (step + 1);
    }

    static int stepCount(EmailFlow flow) {
        return switch (flow) {
            case WELCOME -> 5;
            case BROWSE_ABANDONMENT, POST_PURCHASE -> 2;
            case CART_ABANDONMENT, WINBACK, SUNSET -> 3;
            case REPLENISHMENT -> 1;
        };
    }

    private static Duration hours(long hours) {
        return Duration.ofHours(hours);
    }
}
