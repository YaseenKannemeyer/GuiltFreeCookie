package za.ac.cput.guiltfreecookie.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientAddressFilterTest {

    @Test
    void stripsThePortAzureLeavesOnTheForwardedAddress() {
        assertEquals("144.126.193.139", ClientAddressFilter.withoutPort("144.126.193.139:46794"));
        assertEquals("2001:db8::1", ClientAddressFilter.withoutPort("[2001:db8::1]:443"));
    }

    @Test
    void leavesBareAddressesAlone() {
        assertEquals("144.126.193.139", ClientAddressFilter.withoutPort("144.126.193.139"));
        assertEquals("2001:db8::1", ClientAddressFilter.withoutPort("2001:db8::1"));
        assertEquals("::1", ClientAddressFilter.withoutPort("::1"));
    }

    @Test
    void laterFiltersSeeTheBareAddress() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/payments/payfast/notify");
        request.setRemoteAddr("144.126.193.139:46794");
        MockFilterChain chain = new MockFilterChain();

        new ClientAddressFilter().doFilter(request, new MockHttpServletResponse(), chain);

        assertEquals("144.126.193.139", chain.getRequest().getRemoteAddr());
    }
}
