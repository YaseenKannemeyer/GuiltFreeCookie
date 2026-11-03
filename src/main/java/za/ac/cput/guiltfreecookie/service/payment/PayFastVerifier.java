package za.ac.cput.guiltfreecookie.service.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

// The two checks that need the network: is the caller really PayFast, and does PayFast confirm the data.
@Component
public class PayFastVerifier {

    private static final Logger log = LoggerFactory.getLogger(PayFastVerifier.class);
    // PayFast's documented notification sources.
    static final List<String> PAYFAST_HOSTS = List.of(
            "www.payfast.co.za", "sandbox.payfast.co.za", "w1w.payfast.co.za", "w2w.payfast.co.za");

    private final PayFastGateway gateway;
    private final boolean verifySourceIp;
    private final boolean verifyWithServer;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public PayFastVerifier(
            PayFastGateway gateway,
            @Value("${app.payfast.verify-source-ip}") boolean verifySourceIp,
            @Value("${app.payfast.verify-with-server}") boolean verifyWithServer) {
        this.gateway = gateway;
        this.verifySourceIp = verifySourceIp;
        this.verifyWithServer = verifyWithServer;
    }

    public boolean isFromPayFast(String remoteAddress) {
        if (!verifySourceIp) {
            return true;
        }
        for (String host : PAYFAST_HOSTS) {
            try {
                for (InetAddress address : InetAddress.getAllByName(host)) {
                    if (address.getHostAddress().equals(remoteAddress)) {
                        return true;
                    }
                }
            } catch (UnknownHostException e) {
                log.warn("Could not resolve {}", host);
            }
        }
        return false;
    }

    // Sends the notification back to PayFast, which answers VALID if it really sent it.
    public boolean confirmedByPayFast(String paramString) {
        if (!verifyWithServer) {
            return true;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://" + gateway.host() + "/eng/query/validate"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(paramString))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().trim().equals("VALID");
        } catch (IOException e) {
            log.warn("PayFast validation request failed", e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
