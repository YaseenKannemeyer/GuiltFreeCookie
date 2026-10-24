package za.ac.cput.guiltfreecookie.service.catalog;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.InventoryMovement;
import za.ac.cput.guiltfreecookie.domain.InventoryReason;
import za.ac.cput.guiltfreecookie.dto.catalog.StockAdjustRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.InventoryMovementRepository;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class InventoryService {

    static final int MAX_STOCK = 1_000_000;
    static final int MAX_NOTE_LENGTH = 255;
    static final int MAX_HISTORY = 200;
    // Only the order flow (Phase 4) records these.
    private static final Set<InventoryReason> SYSTEM_REASONS =
            EnumSet.of(InventoryReason.ORDER_RESERVED, InventoryReason.ORDER_RELEASED, InventoryReason.REFUNDED);

    private final CookieRepository cookieRepository;
    private final InventoryMovementRepository movementRepository;

    public InventoryService(CookieRepository cookieRepository, InventoryMovementRepository movementRepository) {
        this.cookieRepository = cookieRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public Cookie adjust(String cookieId, StockAdjustRequest request, String actor) {
        if ((request.adjustBy() == null) == (request.setTo() == null)) {
            throw new BadRequestException("Send either adjustBy or setTo");
        }
        if (request.reason() != null && SYSTEM_REASONS.contains(request.reason())) {
            throw new BadRequestException("That reason is recorded automatically by orders");
        }
        String note = Helper.clean(request.note());
        if (!Helper.isWithin(note, MAX_NOTE_LENGTH)) {
            throw new BadRequestException("Note must be at most " + MAX_NOTE_LENGTH + " characters");
        }
        Cookie cookie = cookieRepository.findById(cookieId).orElseThrow(() -> new NotFoundException("Cookie not found"));

        int change;
        InventoryReason reason;
        if (request.adjustBy() != null) {
            change = request.adjustBy();
            if (change == 0 || Math.abs(change) > MAX_STOCK) {
                throw new BadRequestException("adjustBy must be a non-zero amount up to " + MAX_STOCK);
            }
            if (cookieRepository.adjustStock(cookieId, change) == 0) {
                throw new ConflictException("Only " + cookie.getStockQuantity() + " in stock: can't remove " + -change);
            }
            reason = request.reason() != null ? request.reason() : (change > 0 ? InventoryReason.RESTOCK : InventoryReason.ADJUSTMENT);
        } else {
            int setTo = request.setTo();
            if (setTo < 0 || setTo > MAX_STOCK) {
                throw new BadRequestException("setTo must be between 0 and " + MAX_STOCK);
            }
            change = setTo - cookie.getStockQuantity();
            cookieRepository.setStock(cookieId, setTo);
            reason = request.reason() != null ? request.reason() : InventoryReason.STOCKTAKE;
        }

        Cookie updated = cookieRepository.findById(cookieId).orElseThrow();
        record(cookieId, change, updated.getStockQuantity(), reason, note, actor);
        return updated;
    }

    public List<InventoryMovement> history(String cookieId, int limit) {
        if (!cookieRepository.existsById(cookieId)) {
            throw new NotFoundException("Cookie not found");
        }
        return movementRepository.findByCookieIdOrderByCreatedAtDesc(
                cookieId, PageRequest.of(0, Math.clamp(limit, 1, MAX_HISTORY)));
    }

    void record(String cookieId, int change, int stockAfter, InventoryReason reason, String note, String actor) {
        movementRepository.save(new InventoryMovement.Builder()
                .setCookieId(cookieId)
                .setQuantityChange(change)
                .setStockAfter(stockAfter)
                .setReason(reason)
                .setNote(note)
                .setActor(actor)
                .setCreatedAt(LocalDateTime.now())
                .build());
    }
}
