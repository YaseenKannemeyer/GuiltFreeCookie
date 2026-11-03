package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Every payment notification received from the gateway, valid ones only.
@Entity
@Table(name = "payments",
        indexes = @Index(columnList = "order_id"))
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(nullable = false, length = 20)
    private String gateway;

    @Column(name = "gateway_reference", length = 100)
    private String gatewayReference;

    @Column(name = "gateway_status", length = 30)
    private String gatewayStatus;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    // Everything the gateway sent, for audits and disputes.
    @Column(name = "raw_payload", length = 4000)
    private String rawPayload;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    protected Payment() {
    }

    private Payment(Builder builder) {
        this.paymentId = builder.paymentId;
        this.orderId = builder.orderId;
        this.gateway = builder.gateway;
        this.gatewayReference = builder.gatewayReference;
        this.gatewayStatus = builder.gatewayStatus;
        this.amount = builder.amount;
        this.rawPayload = builder.rawPayload;
        this.receivedAt = builder.receivedAt;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getGateway() {
        return gateway;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public String getGatewayStatus() {
        return gatewayStatus;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public static class Builder {
        private String paymentId;
        private String orderId;
        private String gateway;
        private String gatewayReference;
        private String gatewayStatus;
        private BigDecimal amount;
        private String rawPayload;
        private LocalDateTime receivedAt;

        public Builder setPaymentId(String paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        public Builder setOrderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder setGateway(String gateway) {
            this.gateway = gateway;
            return this;
        }

        public Builder setGatewayReference(String gatewayReference) {
            this.gatewayReference = gatewayReference;
            return this;
        }

        public Builder setGatewayStatus(String gatewayStatus) {
            this.gatewayStatus = gatewayStatus;
            return this;
        }

        public Builder setAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder setRawPayload(String rawPayload) {
            this.rawPayload = rawPayload;
            return this;
        }

        public Builder setReceivedAt(LocalDateTime receivedAt) {
            this.receivedAt = receivedAt;
            return this;
        }

        public Builder copy(Payment payment) {
            this.paymentId = payment.paymentId;
            this.orderId = payment.orderId;
            this.gateway = payment.gateway;
            this.gatewayReference = payment.gatewayReference;
            this.gatewayStatus = payment.gatewayStatus;
            this.amount = payment.amount;
            this.rawPayload = payment.rawPayload;
            this.receivedAt = payment.receivedAt;
            return this;
        }

        public Payment build() {
            return new Payment(this);
        }
    }
}
