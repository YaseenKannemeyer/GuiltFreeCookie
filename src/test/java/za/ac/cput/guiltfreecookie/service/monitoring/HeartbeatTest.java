package za.ac.cput.guiltfreecookie.service.monitoring;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartbeatTest {

    private HttpServer server;
    private final AtomicInteger hits = new AtomicInteger();
    private String base;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> {
            hits.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.createContext("/broken", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private Heartbeat heartbeat(String reservations, String daily) {
        Map<Heartbeat.Job, String> urls = new HashMap<>();
        urls.put(Heartbeat.Job.RESERVATIONS, reservations);
        urls.put(Heartbeat.Job.DAILY_FLOWS, daily);
        return new Heartbeat(urls, HttpClient.newHttpClient());
    }

    @Test
    void pingsTheJobsUrl() throws Exception {
        assertTrue(heartbeat(base + "/ok", "").ping(Heartbeat.Job.RESERVATIONS).get(5, TimeUnit.SECONDS));
        assertEquals(1, hits.get());
    }

    @Test
    void doesNothingWithoutAUrl() throws Exception {
        assertFalse(heartbeat(base + "/ok", " ").ping(Heartbeat.Job.DAILY_FLOWS).get(5, TimeUnit.SECONDS));
        assertFalse(heartbeat(null, null).ping(Heartbeat.Job.RESERVATIONS).get(5, TimeUnit.SECONDS));
        assertEquals(0, hits.get());
    }

    @Test
    void anErrorAnswerOrUnreachableMonitorIsLoggedNotThrown() throws Exception {
        assertFalse(heartbeat(base + "/broken", "").ping(Heartbeat.Job.RESERVATIONS).get(5, TimeUnit.SECONDS));
        server.stop(0);
        assertFalse(heartbeat(base + "/ok", "").ping(Heartbeat.Job.RESERVATIONS).get(5, TimeUnit.SECONDS));
        assertFalse(heartbeat("not a url", "").ping(Heartbeat.Job.RESERVATIONS).get(5, TimeUnit.SECONDS));
    }
}
