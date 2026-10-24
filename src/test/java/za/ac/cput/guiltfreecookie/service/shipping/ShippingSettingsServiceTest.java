package za.ac.cput.guiltfreecookie.service.shipping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.domain.ShippingSettings;
import za.ac.cput.guiltfreecookie.dto.shipping.ShippingQuote;
import za.ac.cput.guiltfreecookie.dto.shipping.ShippingSettingsRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.repository.ShippingSettingsRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingSettingsServiceTest {

    @Mock
    private ShippingSettingsRepository repository;
    private ShippingSettingsService service;

    @BeforeEach
    void setUp() {
        service = new ShippingSettingsService(repository, new BigDecimal("80.00"), new BigDecimal("60.00"),
                new BigDecimal("500.00"), "1 Bakery Lane", "Bring your order number.");
        when(repository.findById(ShippingSettings.DEFAULT_ID)).thenReturn(Optional.empty());
        when(repository.save(any(ShippingSettings.class))).thenAnswer(call -> call.getArgument(0));
    }

    private ShippingSettingsRequest request(Boolean courier, Boolean pudo, Boolean pickup, String pickupAddress, String threshold) {
        return new ShippingSettingsRequest(courier, new BigDecimal("90"), "1–2 days", pudo, new BigDecimal("55"), null,
                pickup, pickupAddress, null, null, threshold == null ? null : new BigDecimal(threshold));
    }

    @Test
    void quotesUseTheConfiguredDefaultsAndFreeThreshold() {
        RuleBasedShippingProvider provider = new RuleBasedShippingProvider(service);

        List<ShippingQuote> belowThreshold = provider.quotes(new BigDecimal("499.99"), false);
        List<ShippingQuote> atThreshold = provider.quotes(new BigDecimal("500.00"), false);

        assertEquals(new BigDecimal("80.00"), belowThreshold.getFirst().cost());
        assertEquals(ShippingMethod.PICKUP, belowThreshold.getLast().method());
        assertEquals(new BigDecimal("0.00"), atThreshold.getFirst().cost());
        assertTrue(atThreshold.getFirst().freeShipping());
        assertTrue(provider.quotes(BigDecimal.ONE, true).get(1).freeShipping());
    }

    @Test
    void anEmptyThresholdSwitchesFreeDeliveryOff() {
        assertNull(service.update(request(true, true, true, "1 Bakery Lane", null)).getFreeShippingThreshold());
    }

    @Test
    void atLeastOneOptionStaysOnAndPickupNeedsAnAddress() {
        assertThrows(BadRequestException.class, () -> service.update(request(false, false, false, "1 Bakery Lane", "500")));
        assertThrows(BadRequestException.class, () -> service.update(request(true, true, true, " ", "500")));
        assertThrows(BadRequestException.class, () -> service.update(new ShippingSettingsRequest(true, new BigDecimal("-1"),
                null, true, new BigDecimal("55"), null, true, "1 Bakery Lane", null, null, null)));
    }
}
