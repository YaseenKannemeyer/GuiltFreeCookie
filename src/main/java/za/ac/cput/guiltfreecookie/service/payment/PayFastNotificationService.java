package za.ac.cput.guiltfreecookie.service.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.Payment;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.PaymentRepository;
import za.ac.cput.guiltfreecookie.service.order.OrderService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// PayFast's Instant Transaction Notification (ITN). An order is only marked paid after PayFast's four checks:
// signature, source address, amount, and PayFast confirming the notification itself.
@Service
public class PayFastNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PayFastNotificationService.class);
    static final int MAX_PAYLOAD_LENGTH = 4000;

    private final PayFastGateway gateway;
    private final PayFastVerifier verifier;
    private final CustomerOrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;

    public PayFastNotificationService(PayFastGateway gateway, PayFastVerifier verifier,
                                      CustomerOrderRepository orderRepository, PaymentRepository paymentRepository,
                                      OrderService orderService) {
        this.gateway = gateway;
        this.verifier = verifier;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
    }

    // params must be in the order PayFast sent them. Returns false for anything that isn't a genuine
    // PayFast notification; true once handled (or safely ignored), so PayFast stops retrying.
    public boolean handle(Map<String, String> params, String remoteAddress) {
        Map<String, String> signed = new LinkedHashMap<>(params);
        String signature = signed.remove("signature");
        String paramString = PayFastSignature.paramString(signed, true);
        String expected = PayFastSignature.sign(paramString, gateway.passphrase());
        if (signature == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            log.warn("PayFast notification rejected: bad signature from {}", remoteAddress);
            return false;
        }
        if (!gateway.merchantId().equals(params.get("merchant_id"))) {
            log.warn("PayFast notification rejected: merchant {} isn't ours", params.get("merchant_id"));
            return false;
        }
        if (!verifier.isFromPayFast(remoteAddress)) {
            log.warn("PayFast notification rejected: {} isn't a PayFast address", remoteAddress);
            return false;
        }

        String orderNumber = params.get("m_payment_id");
        CustomerOrder order = orderNumber == null ? null : orderRepository.findByOrderNumber(orderNumber).orElse(null);
        if (order == null) {
            log.warn("PayFast notification for unknown order {}", orderNumber);
            return true;
        }
        BigDecimal amount = amount(params.get("amount_gross"));
        if (amount == null || amount.compareTo(order.getTotal()) != 0) {
            log.warn("PayFast notification for {}: amount {} doesn't match total {}", orderNumber, amount, order.getTotal());
            record(order, params, "AMOUNT_MISMATCH", amount, paramString);
            return true;
        }
        if (!verifier.confirmedByPayFast(paramString)) {
            log.warn("PayFast did not confirm the notification for {}", orderNumber);
            return false;
        }

        String status = params.getOrDefault("payment_status", "");
        record(order, params, status, amount, paramString);
        String reference = params.getOrDefault("pf_payment_id", "");
        switch (status) {
            case "COMPLETE" -> orderService.markPaid(orderNumber, "payfast", "Paid with PayFast (ref " + reference + ")");
            case "FAILED" -> orderService.markPaymentFailed(orderNumber, "payfast", "PayFast reported the payment failed");
            case "CANCELLED" -> orderService.markPaymentFailed(orderNumber, "payfast", "The payment was cancelled at PayFast");
            default -> log.info("PayFast status {} for {}: nothing to do yet", status, orderNumber);
        }
        return true;
    }

    private void record(CustomerOrder order, Map<String, String> params, String status, BigDecimal amount, String payload) {
        paymentRepository.save(new Payment.Builder()
                .setOrderId(order.getOrderId())
                .setGateway("PAYFAST")
                .setGatewayReference(params.get("pf_payment_id"))
                .setGatewayStatus(status)
                .setAmount(amount)
                .setRawPayload(payload.length() > MAX_PAYLOAD_LENGTH ? payload.substring(0, MAX_PAYLOAD_LENGTH) : payload)
                .setReceivedAt(LocalDateTime.now())
                .build());
    }

    private static BigDecimal amount(String value) {
        try {
            return value == null ? null : new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
