package za.ac.cput.guiltfreecookie.service.payment;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayFastSignatureTest {

    @Test
    void encodesLikePhpUrlencode() {
        assertEquals("Box+of+6+%2A+2", PayFastSignature.encode("Box of 6 * 2"));
        assertEquals("https%3A%2F%2Fshop.example%2Forder%3Fa%3D1%26b%3D2", PayFastSignature.encode("https://shop.example/order?a=1&b=2"));
        assertEquals("caf%C3%A9-_.%7E", PayFastSignature.encode("café-_.~"));
    }

    @Test
    void checkoutStringsSkipBlanksAndKeepTheGivenOrder() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", "10000100");
        fields.put("name_first", " Thandi ");
        fields.put("cell_number", "");
        fields.put("amount", "420.00");

        assertEquals("merchant_id=10000100&name_first=Thandi&amount=420.00", PayFastSignature.paramString(fields, false));
        assertEquals("merchant_id=10000100&name_first=Thandi&cell_number=&amount=420.00", PayFastSignature.paramString(fields, true));
    }

    // Expected values computed independently: md5 of the exact strings below.
    @Test
    void signsWithAndWithoutAPassphrase() {
        String paramString = "merchant_id=10000100&merchant_key=46f0cd694581a&amount=100.00&item_name=Test+Item";

        assertEquals("7abbb23afc89fb75f1412d1f9e5bf7bc", PayFastSignature.sign(paramString, null));
        assertEquals("711830950e3c917da00a3193efecdfb8", PayFastSignature.sign(paramString, "jt7NOE43FZPn"));
    }

    @Test
    void cellNumbersAreSentInLocalFormat() {
        assertEquals("0821234567", PayFastGateway.cellNumber("+27 82 123 4567"));
        assertEquals("0821234567", PayFastGateway.cellNumber("082 123 4567"));
        assertEquals(null, PayFastGateway.cellNumber("12345"));
    }
}
