package za.ac.cput.guiltfreecookie.service.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.dto.catalog.VariantRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CookieVariantRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CookieVariantServiceTest {

    @Mock
    private CookieVariantRepository variantRepository;
    @Mock
    private CookieRepository cookieRepository;
    @InjectMocks
    private CookieVariantService service;

    private final Cookie cookie = new Cookie.Builder().setCookieId("CK1").setPrice(32.00).build();

    @Test
    void anUnpricedPackCostsUnitPriceTimesCookies() {
        CookieVariant boxOfSix = new CookieVariant.Builder().setUnitsPerPack(6).build();
        CookieVariant boxOfTwelve = new CookieVariant.Builder().setUnitsPerPack(12).setPrice(new BigDecimal("330")).build();

        assertEquals(new BigDecimal("192.00"), boxOfSix.effectivePrice(cookie));
        assertEquals(new BigDecimal("330.00"), boxOfTwelve.effectivePrice(cookie));
    }

    @Test
    void createSavesACleanedPack() {
        when(cookieRepository.existsById("CK1")).thenReturn(true);
        when(variantRepository.save(any(CookieVariant.class))).thenAnswer(call -> call.getArgument(0));

        CookieVariant created = service.create("CK1", new VariantRequest("  Box of 6 ", 6, new BigDecimal("180.00"), null, null));

        assertEquals("Box of 6", created.getLabel());
        assertEquals(6, created.getSortOrder());
        assertEquals(new BigDecimal("180.00"), created.getPrice());
    }

    @Test
    void createRejectsBadInput() {
        when(cookieRepository.existsById("CK1")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.create("CK1", new VariantRequest(" ", 6, null, null, null)));
        assertThrows(BadRequestException.class, () -> service.create("CK1", new VariantRequest("Box", 0, null, null, null)));
        assertThrows(BadRequestException.class, () -> service.create("CK1", new VariantRequest("Box", 101, null, null, null)));
        assertThrows(BadRequestException.class, () -> service.create("CK1", new VariantRequest("Box", 6, new BigDecimal("-1"), null, null)));
        assertThrows(BadRequestException.class, () -> service.create("CK1", new VariantRequest("Box", 6, new BigDecimal("1.005"), null, null)));
        verify(variantRepository, never()).save(any());
    }

    @Test
    void createRejectsADuplicateLabel() {
        when(cookieRepository.existsById("CK1")).thenReturn(true);
        when(variantRepository.existsByCookieIdAndLabelIgnoreCase("CK1", "Single")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create("CK1", new VariantRequest("Single", 1, null, null, null)));
    }

    @Test
    void createNeedsAnExistingCookie() {
        when(cookieRepository.existsById("nope")).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.create("nope", new VariantRequest("Box", 6, null, null, null)));
    }

    @Test
    void aPackFromAnotherCookieIsNotFound() {
        CookieVariant otherCookies = new CookieVariant.Builder().setVariantId("V9").setCookieId("CK2").setLabel("Box").build();
        when(variantRepository.findById("V9")).thenReturn(Optional.of(otherCookies));

        assertThrows(NotFoundException.class, () -> service.deactivate("CK1", "V9"));
    }

    @Test
    void deactivateHidesThePackInsteadOfDeletingIt() {
        CookieVariant pack = new CookieVariant.Builder().setVariantId("V1").setCookieId("CK1").setLabel("Box").build();
        when(variantRepository.findById("V1")).thenReturn(Optional.of(pack));
        when(variantRepository.save(any(CookieVariant.class))).thenAnswer(call -> call.getArgument(0));

        assertFalse(service.deactivate("CK1", "V1").isActive());
        verify(variantRepository, never()).delete(any());
    }

    @Test
    void ensureDefaultAddsAnUnpricedSingleOnlyWhenThereAreNoPacks() {
        when(variantRepository.existsByCookieId("CK1")).thenReturn(false);

        service.ensureDefault(cookie);

        ArgumentCaptor<CookieVariant> saved = ArgumentCaptor.forClass(CookieVariant.class);
        verify(variantRepository).save(saved.capture());
        assertEquals("Single", saved.getValue().getLabel());
        assertEquals(1, saved.getValue().getUnitsPerPack());
        assertNull(saved.getValue().getPrice());
    }
}
