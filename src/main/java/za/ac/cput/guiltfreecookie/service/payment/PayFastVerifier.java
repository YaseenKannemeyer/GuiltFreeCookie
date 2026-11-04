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
    // PayFast's published ITN source ranges. The sandbox sends from 144.126.193.139, which none of the hosts resolve to.
    static final List<String> PAYFAST_RANGES = List.of(
            "197.97.145.144/28", "41.74.179.192/27", "102.216.36.0/28", "102.216.36.128/28", "144.126.193.139/32");

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
        if (inPublishedRange(remoteAddress)) {
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

    static boolean inPublishedRange(String remoteAddress) {
        byte[] address = ipv4(remoteAddress);
        if (address == null) {
            return false;
        }
        for (String range : PAYFAST_RANGES) {
            int slash = range.indexOf('/');
            byte[] network = ipv4(range.substring(0, slash));
            int prefix = Integer.parseInt(range.substring(slash + 1));
            int mask = prefix == 0 ? 0 : -1 << (32 - prefix);
            if ((toInt(address) & mask) == (toInt(network) & mask)) {
                return true;
            }
        }
        return false;
    }

    // Parses dotted IPv4 only, without any DNS lookup.
    private static byte[] ipv4(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.trim().split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            if (parts[i].isEmpty() || parts[i].length() > 3 || !parts[i].chars().allMatch(Character::isDigit)) {
                return null;
            }
            int octet = Integer.parseInt(parts[i]);
            if (octet > 255) {
                return null;
            }
            bytes[i] = (byte) octet;
        }
        return bytes;
    }

    private static int toInt(byte[] b) {
        return (b[0] & 0xff) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
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
