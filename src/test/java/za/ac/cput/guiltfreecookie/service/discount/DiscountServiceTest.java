package za.ac.cput.guiltfreecookie.service.discount;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.guiltfreecookie.domain.DiscountCode;
import za.ac.cput.guiltfreecookie.domain.DiscountType;
import za.ac.cput.guiltfreecookie.dto.discount.DiscountRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.repository.DiscountCodeRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private DiscountCodeRepository repository;
    @InjectMocks
    private DiscountService service;

    private final LocalDateTime now = LocalDateTime.now();
    private final BigDecimal subtotal = new BigDecimal("200.00");

    private DiscountCode.Builder code(DiscountType type, String value) {
        return new DiscountCode.Builder().setCode("SAVE").setType(type).setValue(value == null ? null : new BigDecimal(value));
    }

    @Test
    void amountsAreWorkedOutPerType() {
        assertEquals(new BigDecimal("30.00"), DiscountService.amount(code(DiscountType.PERCENT, "15").build(), subtotal));
        assertEquals(new BigDecimal("50.00"), DiscountService.amount(code(DiscountType.FIXED, "50").build(), subtotal));
        assertEquals(new BigDecimal("200.00"), DiscountService.amount(code(DiscountType.FIXED, "500").build(), subtotal));
        assertEquals(new BigDecimal("0.00"), DiscountService.amount(code(DiscountType.FREE_SHIPPING, null).build(), subtotal));
    }

    @Test
    void problemsAreExplainedInPlainWords() {
        DiscountCode ok = code(DiscountType.PERCENT, "10").build();
        assertNull(DiscountService.problem(ok, subtotal, null, now));
        assertEquals(DiscountService.NOT_VALID, DiscountService.problem(null, subtotal, null, now));
        assertEquals(DiscountService.NOT_VALID, DiscountService.problem(code(DiscountType.PERCENT, "10").setActive(false).build(), subtotal, null, now));
        assertEquals("This code isn't active yet",
                DiscountService.problem(code(DiscountType.PERCENT, "10").setStartsAt(now.plusDays(1)).build(), subtotal, null, now));
        assertEquals("This code has expired",
                DiscountService.problem(code(DiscountType.PERCENT, "10").setEndsAt(now.minusMinutes(1)).build(), subtotal, null, now));
        assertEquals("This code has already been used",
                DiscountService.problem(code(DiscountType.PERCENT, "10").setMaxUses(1).setUsedCount(1).build(), subtotal, null, now));
        assertEquals("Spend R250.00 or more to use this code",
                DiscountService.problem(code(DiscountType.PERCENT, "10").setMinSubtotal(new BigDecimal("250")).build(), subtotal, null, now));
    }

    @Test
    void personalCodesOnlyWorkForTheirEmail() {
        DiscountCode personal = code(DiscountType.PERCENT, "10").setRestrictedToEmail("thandi@example.co.za").build();

        assertNull(DiscountService.problem(personal, subtotal, "Thandi@Example.co.za", now));
        assertEquals("This code belongs to a different email address", DiscountService.problem(personal, subtotal, "x@example.co.za", now));
        assertEquals(DiscountService.EMAIL_NEEDED, DiscountService.problem(personal, subtotal, null, now));
    }

    @Test
    void aPersonalCodeCanGoInACartBeforeTheEmailIsKnownButNotTheWrongEmail() {
        when(repository.findByCode("WELCOME-ABC234")).thenReturn(Optional.of(
                code(DiscountType.PERCENT, "10").setCode("WELCOME-ABC234").setRestrictedToEmail("thandi@example.co.za").build()));

        assertEquals("WELCOME-ABC234", service.requireApplicable("welcome-abc234", subtotal, null).getCode());
        assertThrows(BadRequestException.class, () -> service.requireApplicable("WELCOME-ABC234", subtotal, "x@example.co.za"));
    }

    @Test
    void evaluateKeepsTheReasonInsteadOfThrowing() {
        when(repository.findByCode("SAVE")).thenReturn(Optional.of(
                code(DiscountType.PERCENT, "10").setMinSubtotal(new BigDecimal("500")).build()));

        DiscountService.Evaluation evaluation = service.evaluate("SAVE", subtotal, null);

        assertFalse(evaluation.applied());
        assertEquals(new BigDecimal("0.00"), evaluation.amount());
        assertTrue(evaluation.message().startsWith("Spend R500.00"));
    }

    @Test
    void requireApplicableNormalisesTheCode() {
        when(repository.findByCode("SAVE")).thenReturn(Optional.of(code(DiscountType.FIXED, "20").build()));

        assertEquals("SAVE", service.requireApplicable("  save ", subtotal, null).getCode());
        assertThrows(BadRequestException.class, () -> service.requireApplicable("  ", subtotal, null));
    }

    @Test
    void createValidatesTheRules() {
        assertThrows(BadRequestException.class, () -> service.create(request("x", DiscountType.PERCENT, "10")));
        assertThrows(BadRequestException.class, () -> service.create(request("SAVE10", DiscountType.PERCENT, "150")));
        assertThrows(BadRequestException.class, () -> service.create(request("SAVE10", DiscountType.FIXED, "0")));
        assertThrows(BadRequestException.class, () -> service.create(request("SAVE10", null, "10")));
        assertThrows(BadRequestException.class, () -> service.create(new DiscountRequest("SAVE10", DiscountType.PERCENT,
                BigDecimal.TEN, null, now, now.minusDays(1), null, null, null, null, null)));
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsADuplicateCode() {
        when(repository.existsByCode("SAVE10")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(request("save10", DiscountType.PERCENT, "10")));
    }

    @Test
    void freeShippingIgnoresAnyValue() {
        when(repository.save(any(DiscountCode.class))).thenAnswer(call -> call.getArgument(0));

        DiscountCode created = service.create(request("SHIPFREE", DiscountType.FREE_SHIPPING, "99"));

        assertNull(created.getValue());
        assertTrue(created.isActive());
    }

    private DiscountRequest request(String code, DiscountType type, String value) {
        return new DiscountRequest(code, type, value == null ? null : new BigDecimal(value), null, null, null, null, null, null, null, null);
    }
}
