package za.ac.cput.guiltfreecookie.service.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.dto.order.PaymentRedirect;
import za.ac.cput.guiltfreecookie.service.order.OrderLinks;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

// Cards, Instant EFT, SnapScan and wallets all happen on PayFast's page, including 3-D Secure:
// card details never reach this server.
@Service
public class PayFastGateway implements PaymentGateway {

    static final String SANDBOX_HOST = "sandbox.payfast.co.za";
    static final String LIVE_HOST = "www.payfast.co.za";
    static final int MAX_NAME_LENGTH = 100;

    private final OrderLinks orderLinks;
    private final boolean sandbox;
    private final String merchantId;
    private final String merchantKey;
    private final String passphrase;
    private final String notifyUrl;

    public PayFastGateway(
            OrderLinks orderLinks,
            @Value("${app.payfast.sandbox}") boolean sandbox,
            @Value("${app.payfast.merchant-id}") String merchantId,
            @Value("${app.payfast.merchant-key}") String merchantKey,
            @Value("${app.payfast.passphrase}") String passphrase,
            @Value("${app.payfast.notify-url}") String notifyUrl) {
        // Without a passphrase the signature is a plain MD5 anyone can compute, so live payments refuse to start.
        if (!sandbox && (passphrase == null || passphrase.isBlank())) {
            throw new IllegalStateException("PAYFAST_PASSPHRASE must be set for live payments");
        }
        this.orderLinks = orderLinks;
        this.sandbox = sandbox;
        this.merchantId = merchantId;
        this.merchantKey = merchantKey;
        this.passphrase = passphrase;
        this.notifyUrl = notifyUrl;
    }

    @Override
    public PaymentRedirect redirectFor(CustomerOrder order) {
        // PayFast checks the signature over the fields in exactly this order.
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", merchantId);
        fields.put("merchant_key", merchantKey);
        fields.put("return_url", orderLinks.orderPage(order));
        fields.put("cancel_url", orderLinks.paymentCancelled(order));
        fields.put("notify_url", notifyUrl);
        fields.put("name_first", truncate(order.getFirstName()));
        fields.put("name_last", truncate(order.getLastName()));
        fields.put("email_address", order.getCustomerEmail());
        fields.put("cell_number", cellNumber(order.getPhone()));
        fields.put("m_payment_id", order.getOrderNumber());
        fields.put("amount", order.getTotal().setScale(2, RoundingMode.HALF_UP).toPlainString());
        fields.put("item_name", "GuiltFreeCookie order " + order.getOrderNumber());
        fields.values().removeIf(value -> value == null || value.isBlank());
        fields.put("signature", PayFastSignature.sign(PayFastSignature.paramString(fields, false), passphrase));
        return new PaymentRedirect("PAYFAST", processUrl(), fields);
    }

    public String host() {
        return sandbox ? SANDBOX_HOST : LIVE_HOST;
    }

    public String merchantId() {
        return merchantId;
    }

    public String passphrase() {
        return passphrase;
    }

    String processUrl() {
        return "https://" + host() + "/eng/process";
    }

    // PayFast wants a local number like 0821234567.
    static String cellNumber(String phone) {
        if (!Helper.isValidMobileNumber(phone)) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        return digits.startsWith("27") ? "0" + digits.substring(2) : digits;
    }

    private static String truncate(String value) {
        return value == null || value.length() <= MAX_NAME_LENGTH ? value : value.substring(0, MAX_NAME_LENGTH);
    }
}
