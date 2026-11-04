package za.ac.cput.guiltfreecookie.service.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// Tells an uptime monitor (Better Stack heartbeats) that a scheduled job finished. A job that stops running
// stops pinging, and the monitor raises the alarm. Does nothing for a job without a URL.
@Component
public class Heartbeat {

    public enum Job { RESERVATIONS, DAILY_FLOWS }

    private static final Logger log = LoggerFactory.getLogger(Heartbeat.class);
    static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final Map<Job, URI> urls = new EnumMap<>(Job.class);
    private final HttpClient client;

    @Autowired
    public Heartbeat(@Value("${app.heartbeat.reservations-url:}") String reservationsUrl,
                     @Value("${app.heartbeat.daily-flows-url:}") String dailyFlowsUrl) {
        this(Map.of(Job.RESERVATIONS, reservationsUrl, Job.DAILY_FLOWS, dailyFlowsUrl),
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    Heartbeat(Map<Job, String> urls, HttpClient client) {
        this.client = client;
        urls.forEach((job, url) -> {
            if (url == null || url.isBlank()) {
                return;
            }
            try {
                this.urls.put(job, URI.create(url.trim()));
            } catch (IllegalArgumentException e) {
                log.warn("Heartbeat URL for {} isn't a valid URL, so it won't be pinged", job);
            }
        });
    }

    // Never throws and never holds up the job: the request runs in the background.
    public CompletableFuture<Boolean> ping(Job job) {
        URI url = urls.get(job);
        if (url == null) {
            return CompletableFuture.completedFuture(false);
        }
        HttpRequest request = HttpRequest.newBuilder(url).timeout(TIMEOUT).GET().build();
        try {
            return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .handle((response, error) -> {
                        if (error != null) {
                            log.warn("Heartbeat for {} failed: {}", job, error.toString());
                            return false;
                        }
                        if (response.statusCode() >= 300) {
                            log.warn("Heartbeat for {} answered {}", job, response.statusCode());
                            return false;
                        }
                        return true;
                    });
        } catch (RuntimeException e) {
            log.warn("Heartbeat for {} failed: {}", job, e.toString());
            return CompletableFuture.completedFuture(false);
        }
    }
}
