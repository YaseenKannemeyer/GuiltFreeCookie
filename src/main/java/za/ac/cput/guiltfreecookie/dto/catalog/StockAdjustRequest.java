package za.ac.cput.guiltfreecookie.dto.catalog;

import za.ac.cput.guiltfreecookie.domain.InventoryReason;

// Send exactly one of adjustBy (+/- change) or setTo (stocktake count).
public record StockAdjustRequest(Integer adjustBy, Integer setTo, InventoryReason reason, String note) {
}
