package za.ac.cput.guiltfreecookie.service.payment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayFastVerifierTest {

    @Test
    void acceptsAddressesInsidePayFastsPublishedRanges() {
        assertTrue(PayFastVerifier.inPublishedRange("144.126.193.139"));
        assertTrue(PayFastVerifier.inPublishedRange("197.97.145.144"));
        assertTrue(PayFastVerifier.inPublishedRange("197.97.145.159"));
        assertTrue(PayFastVerifier.inPublishedRange("41.74.179.222"));
        assertTrue(PayFastVerifier.inPublishedRange("102.216.36.5"));
        assertTrue(PayFastVerifier.inPublishedRange("102.216.36.140"));
    }

    @Test
    void rejectsAddressesJustOutsideOrMalformed() {
        assertFalse(PayFastVerifier.inPublishedRange("144.126.193.140"));
        assertFalse(PayFastVerifier.inPublishedRange("197.97.145.160"));
        assertFalse(PayFastVerifier.inPublishedRange("41.74.179.224"));
        assertFalse(PayFastVerifier.inPublishedRange("102.216.36.16"));
        assertFalse(PayFastVerifier.inPublishedRange("203.0.113.9"));
        assertFalse(PayFastVerifier.inPublishedRange("144.126.193.139:46794"));
        assertFalse(PayFastVerifier.inPublishedRange("not-an-ip"));
        assertFalse(PayFastVerifier.inPublishedRange(null));
    }

    @Test
    void sandboxNotificationPassesWithoutDnsLookup() {
        PayFastVerifier verifier = new PayFastVerifier(null, true, true);
        assertTrue(verifier.isFromPayFast("144.126.193.139"));
    }
}
