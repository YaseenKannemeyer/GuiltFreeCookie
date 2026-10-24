package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// The order's timeline: every status change, with who made it.
@Entity
@Table(name = "order_status_events",
        indexes = @Index(columnList = "order_id, created_at"))
public class OrderStatusEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(length = 500)
    private String note;

    // Admin id, "customer", "payfast" or "system".
    @Column(length = 254)
    private String actor;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected OrderStatusEvent() {
    }

    private OrderStatusEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.orderId = builder.orderId;
        this.status = builder.status;
        this.note = builder.note;
        this.actor = builder.actor;
        this.createdAt = builder.createdAt;
    }

    public String getEventId() {
        return eventId;
    }

    public String getOrderId() {
        return orderId;
    }

    public OrderStatus getStatus() {
        return status;
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
        private String eventId;
        private String orderId;
        private OrderStatus status;
        private String note;
        private String actor;
        private LocalDateTime createdAt;

        public Builder setEventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder setOrderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder setStatus(OrderStatus status) {
            this.status = status;
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

        public Builder copy(OrderStatusEvent orderStatusEvent) {
            this.eventId = orderStatusEvent.eventId;
            this.orderId = orderStatusEvent.orderId;
            this.status = orderStatusEvent.status;
            this.note = orderStatusEvent.note;
            this.actor = orderStatusEvent.actor;
            this.createdAt = orderStatusEvent.createdAt;
            return this;
        }

        public OrderStatusEvent build() {
            return new OrderStatusEvent(this);
        }
    }
}
