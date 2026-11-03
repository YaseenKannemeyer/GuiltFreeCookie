package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Audit trail: every stock change, who made it and why.
@Entity
@Table(name = "inventory_movements", indexes = @Index(columnList = "cookie_id, created_at"))
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "movement_id")
    private String movementId;

    @Column(name = "cookie_id", nullable = false)
    private String cookieId;

    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    @Column(name = "stock_after", nullable = false)
    private int stockAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InventoryReason reason;

    @Column(length = 255)
    private String note;

    @Column(length = 254)
    private String actor;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected InventoryMovement() {
    }

    private InventoryMovement(Builder builder) {
        this.movementId = builder.movementId;
        this.cookieId = builder.cookieId;
        this.quantityChange = builder.quantityChange;
        this.stockAfter = builder.stockAfter;
        this.reason = builder.reason;
        this.note = builder.note;
        this.actor = builder.actor;
        this.createdAt = builder.createdAt;
    }

    public String getMovementId() {
        return movementId;
    }

    public String getCookieId() {
        return cookieId;
    }

    public int getQuantityChange() {
        return quantityChange;
    }

    public int getStockAfter() {
        return stockAfter;
    }

    public InventoryReason getReason() {
        return reason;
    }

    public String getNote() {
        return note;
    }

    public String getActor() {
        return actor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public static class Builder {
        private String movementId;
        private String cookieId;
        private int quantityChange;
        private int stockAfter;
        private InventoryReason reason;
        private String note;
        private String actor;
        private LocalDateTime createdAt;

        public Builder setMovementId(String movementId) {
            this.movementId = movementId;
            return this;
        }

        public Builder setCookieId(String cookieId) {
            this.cookieId = cookieId;
            return this;
        }

        public Builder setQuantityChange(int quantityChange) {
            this.quantityChange = quantityChange;
            return this;
        }

        public Builder setStockAfter(int stockAfter) {
            this.stockAfter = stockAfter;
            return this;
        }

        public Builder setReason(InventoryReason reason) {
            this.reason = reason;
            return this;
        }

        public Builder setNote(String note) {
            this.note = note;
            return this;
        }

        public Builder setActor(String actor) {
            this.actor = actor;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder copy(InventoryMovement movement) {
            this.movementId = movement.movementId;
            this.cookieId = movement.cookieId;
            this.quantityChange = movement.quantityChange;
            this.stockAfter = movement.stockAfter;
            this.reason = movement.reason;
            this.note = movement.note;
            this.actor = movement.actor;
            this.createdAt = movement.createdAt;
            return this;
        }

        public InventoryMovement build() {
            return new InventoryMovement(this);
        }
    }
}
