package za.ac.cput.guiltfreecookie.service.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.InventoryMovement;
import za.ac.cput.guiltfreecookie.domain.InventoryReason;
import za.ac.cput.guiltfreecookie.dto.catalog.StockAdjustRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.InventoryMovementRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private CookieRepository cookieRepository;
    @Mock
    private InventoryMovementRepository movementRepository;
    @InjectMocks
    private InventoryService service;

    private Cookie withStock(int stock) {
        return new Cookie.Builder().setCookieId("CK1").setStockQuantity(stock).build();
    }

    @Test
    void restockAddsAndLogsWhoDidIt() {
        when(cookieRepository.findById("CK1")).thenReturn(Optional.of(withStock(10)), Optional.of(withStock(34)));
        when(cookieRepository.adjustStock("CK1", 24)).thenReturn(1);

        Cookie result = service.adjust("CK1", new StockAdjustRequest(24, null, null, " Monday bake "), "AD001");

        assertEquals(34, result.getStockQuantity());
        ArgumentCaptor<InventoryMovement> movement = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(movement.capture());
        assertEquals(24, movement.getValue().getQuantityChange());
        assertEquals(34, movement.getValue().getStockAfter());
        assertEquals(InventoryReason.RESTOCK, movement.getValue().getReason());
        assertEquals("Monday bake", movement.getValue().getNote());
        assertEquals("AD001", movement.getValue().getActor());
    }

    @Test
    void removingMoreThanIsInStockIsAConflictAndLogsNothing() {
        when(cookieRepository.findById("CK1")).thenReturn(Optional.of(withStock(3)));
        when(cookieRepository.adjustStock("CK1", -5)).thenReturn(0);

        assertThrows(ConflictException.class, () -> service.adjust("CK1", new StockAdjustRequest(-5, null, null, null), "AD001"));
        verify(movementRepository, never()).save(any());
    }

    @Test
    void stocktakeSetsTheCountAndLogsTheDifference() {
        when(cookieRepository.findById("CK1")).thenReturn(Optional.of(withStock(30)), Optional.of(withStock(26)));

        service.adjust("CK1", new StockAdjustRequest(null, 26, null, null), "AD001");

        verify(cookieRepository).setStock("CK1", 26);
        ArgumentCaptor<InventoryMovement> movement = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(movement.capture());
        assertEquals(-4, movement.getValue().getQuantityChange());
        assertEquals(InventoryReason.STOCKTAKE, movement.getValue().getReason());
    }

    @Test
    void rejectsAmbiguousOrInvalidRequests() {
        assertThrows(BadRequestException.class, () -> service.adjust("CK1", new StockAdjustRequest(null, null, null, null), "AD001"));
        assertThrows(BadRequestException.class, () -> service.adjust("CK1", new StockAdjustRequest(5, 5, null, null), "AD001"));
        assertThrows(BadRequestException.class,
                () -> service.adjust("CK1", new StockAdjustRequest(5, null, InventoryReason.ORDER_RESERVED, null), "AD001"));
        verify(cookieRepository, never()).adjustStock(anyString(), anyInt());
    }

    @Test
    void rejectsAZeroOrNegativeCount() {
        when(cookieRepository.findById("CK1")).thenReturn(Optional.of(withStock(3)));

        assertThrows(BadRequestException.class, () -> service.adjust("CK1", new StockAdjustRequest(0, null, null, null), "AD001"));
        assertThrows(BadRequestException.class, () -> service.adjust("CK1", new StockAdjustRequest(null, -1, null, null), "AD001"));
    }
}
