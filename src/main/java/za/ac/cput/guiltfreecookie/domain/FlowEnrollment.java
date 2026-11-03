package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// One person working through one email flow. currentStep is the next step to send.
@Entity
@Table(name = "flow_enrollments", indexes = {
        @Index(name = "idx_flow_enrollments_due", columnList = "status, next_send_at"),
        @Index(name = "idx_flow_enrollments_email", columnList = "email, flow")})
public class FlowEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "enrollment_id")
    private String enrollmentId;

    @Column(nullable = false, length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmailFlow flow;

    // The cart id, order number or cookie id the flow is about, when it has one.
    @Column(name = "context_ref", length = 100)
    private String contextRef;

    @Column(name = "discount_code", length = 30)
    private String discountCode;

    @Column(name = "current_step")
    private int currentStep;

    @Column(name = "next_send_at")
    private LocalDateTime nextSendAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(name = "exit_reason", length = 100)
    private String exitReason;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected FlowEnrollment() {
    }

    private FlowEnrollment(Builder builder) {
        this.enrollmentId = builder.enrollmentId;
        this.email = builder.email;
        this.flow = builder.flow;
        this.contextRef = builder.contextRef;
        this.discountCode = builder.discountCode;
        this.currentStep = builder.currentStep;
        this.nextSendAt = builder.nextSendAt;
        this.status = builder.status;
        this.exitReason = builder.exitReason;
        this.enrolledAt = builder.enrolledAt;
        this.updatedAt = builder.updatedAt;
    }

    public String getEnrollmentId() {
        return enrollmentId;
    }

    public String getEmail() {
        return email;
    }

    public EmailFlow getFlow() {
        return flow;
    }

    public String getContextRef() {
        return contextRef;
    }

    public String getDiscountCode() {
        return discountCode;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public LocalDateTime getNextSendAt() {
        return nextSendAt;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public String getExitReason() {
        return exitReason;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static class Builder {
        private String enrollmentId;
        private String email;
        private EmailFlow flow;
        private String contextRef;
        private String discountCode;
        private int currentStep;
        private LocalDateTime nextSendAt;
        private EnrollmentStatus status;
        private String exitReason;
        private LocalDateTime enrolledAt;
        private LocalDateTime updatedAt;

        public Builder setEnrollmentId(String enrollmentId) {
            this.enrollmentId = enrollmentId;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setFlow(EmailFlow flow) {
            this.flow = flow;
            return this;
        }

        public Builder setContextRef(String contextRef) {
            this.contextRef = contextRef;
            return this;
        }

        public Builder setDiscountCode(String discountCode) {
            this.discountCode = discountCode;
            return this;
        }

        public Builder setCurrentStep(int currentStep) {
            this.currentStep = currentStep;
            return this;
        }

        public Builder setNextSendAt(LocalDateTime nextSendAt) {
            this.nextSendAt = nextSendAt;
            return this;
        }

        public Builder setStatus(EnrollmentStatus status) {
            this.status = status;
            return this;
        }

        public Builder setExitReason(String exitReason) {
            this.exitReason = exitReason;
            return this;
        }

        public Builder setEnrolledAt(LocalDateTime enrolledAt) {
            this.enrolledAt = enrolledAt;
            return this;
        }

        public Builder setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder copy(FlowEnrollment flowEnrollment) {
            this.enrollmentId = flowEnrollment.enrollmentId;
            this.email = flowEnrollment.email;
            this.flow = flowEnrollment.flow;
            this.contextRef = flowEnrollment.contextRef;
            this.discountCode = flowEnrollment.discountCode;
            this.currentStep = flowEnrollment.currentStep;
            this.nextSendAt = flowEnrollment.nextSendAt;
            this.status = flowEnrollment.status;
            this.exitReason = flowEnrollment.exitReason;
            this.enrolledAt = flowEnrollment.enrolledAt;
            this.updatedAt = flowEnrollment.updatedAt;
            return this;
        }

        public FlowEnrollment build() {
            return new FlowEnrollment(this);
        }
    }
}
