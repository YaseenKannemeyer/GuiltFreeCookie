package za.ac.cput.guiltfreecookie.service.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.Payment;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.PaymentRepository;
import za.ac.cput.guiltfreecookie.service.order.OrderLinks;
import za.ac.cput.guiltfreecookie.service.order.OrderService;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayFastNotificationServiceTest {

    private static final String PASSPHRASE = "jt7NOE43FZPn";

    @Mock
    private PayFastVerifier verifier;
    @Mock
    private CustomerOrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderService orderService;
    private PayFastNotificationService service;

    private final CustomerOrder order = new CustomerOrder.Builder()
            .setOrderId("O1").setOrderNumber("GFC-261024-ABCD").setTotal(new BigDecimal("420.00")).build();

    @BeforeEach
    void setUp() {
        PayFastGateway gateway = new PayFastGateway(new OrderLinks("secret", "https://shop.example"), true,
                "10000100", "46f0cd694581a", PASSPHRASE, "https://shop.example/notify");
        service = new PayFastNotificationService(gateway, verifier, orderRepository, paymentRepository, orderService);
    }

    // A notification as PayFast would send it, signed over every field in order (blank ones included).
    private Map<String, String> notification(String status, String amount) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("m_payment_id", "GFC-261024-ABCD");
        params.put("pf_payment_id", "1089250");
        params.put("payment_status", status);
        params.put("item_name", "GuiltFreeCookie order GFC-261024-ABCD");
        params.put("item_description", "");
        params.put("amount_gross", amount);
        params.put("amount_fee", "-9.66");
        params.put("amount_net", "410.34");
        params.put("name_first", "Thandi");
        params.put("email_address", "thandi@example.co.za");
        params.put("merchant_id", "10000100");
        params.put("signature", PayFastSignature.sign(PayFastSignature.paramString(params, true), PASSPHRASE));
        return params;
    }

    private void genuine() {
        when(verifier.isFromPayFast("196.33.227.224")).thenReturn(true);
        when(orderRepository.findByOrderNumber("GFC-261024-ABCD")).thenReturn(Optional.of(order));
    }

    @Test
    void aCompletePaymentMarksTheOrderPaidAndIsRecorded() {
        genuine();
        when(verifier.confirmedByPayFast(anyString())).thenReturn(true);

        assertTrue(service.handle(notification("COMPLETE", "420.00"), "196.33.227.224"));

        verify(orderService).markPaid(eq("GFC-261024-ABCD"), eq("payfast"), eq("Paid with PayFast (ref 1089250)"));
        verify(paymentRepository).save(argThat((Payment p) -> p.getGatewayStatus().equals("COMPLETE")
                && p.getGatewayReference().equals("1089250") && !p.getRawPayload().contains("passphrase")));
    }

    @Test
    void aFailedPaymentIsPassedOn() {
        genuine();
        when(verifier.confirmedByPayFast(anyString())).thenReturn(true);

        assertTrue(service.handle(notification("FAILED", "420.00"), "196.33.227.224"));

        verify(orderService).markPaymentFailed(eq("GFC-261024-ABCD"), eq("payfast"), anyString());
    }

    @Test
    void aTamperedNotificationIsRejected() {
        Map<String, String> params = notification("COMPLETE", "420.00");
        params.put("amount_gross", "1.00");

        assertFalse(service.handle(params, "196.33.227.224"));
        verifyNoInteractions(orderService, paymentRepository);
    }

    @Test
    void aNotificationNotFromPayFastIsRejected() {
        when(verifier.isFromPayFast("203.0.113.9")).thenReturn(false);

        assertFalse(service.handle(notification("COMPLETE", "420.00"), "203.0.113.9"));
        verifyNoInteractions(orderService);
    }

    @Test
    void theWrongAmountIsLoggedButNeverPaid() {
        genuine();

        assertTrue(service.handle(notification("COMPLETE", "42.00"), "196.33.227.224"));

        verify(orderService, never()).markPaid(anyString(), anyString(), anyString());
        verify(paymentRepository).save(argThat((Payment p) -> p.getGatewayStatus().equals("AMOUNT_MISMATCH")));
    }

    @Test
    void aNotificationPayFastWontConfirmIsRejected() {
        genuine();
        when(verifier.confirmedByPayFast(anyString())).thenReturn(false);

        assertFalse(service.handle(notification("COMPLETE", "420.00"), "196.33.227.224"));
        verify(orderService, never()).markPaid(anyString(), anyString(), anyString());
    }

    @Test
    void anUnknownOrderIsAcknowledgedSoPayFastStopsRetrying() {
        when(verifier.isFromPayFast("196.33.227.224")).thenReturn(true);
        when(orderRepository.findByOrderNumber("GFC-261024-ABCD")).thenReturn(Optional.empty());

        assertTrue(service.handle(notification("COMPLETE", "420.00"), "196.33.227.224"));
        verify(paymentRepository, never()).save(any());
    }
}
