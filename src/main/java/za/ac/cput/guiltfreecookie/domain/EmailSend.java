package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// A flow email that went out. Its id is the unguessable token in the open pixel and tracked links.
@Entity
@Table(name = "email_sends", indexes = {
        @Index(name = "idx_email_sends_email", columnList = "email, sent_at"),
        @Index(name = "idx_email_sends_flow", columnList = "flow, sent_at")})
public class EmailSend {

    @Id
    @Column(name = "send_id", length = 32)
    private String sendId;

    @Column(nullable = false, length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmailFlow flow;

    private int step;

    @Column(name = "context_ref", length = 100)
    private String contextRef;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    protected EmailSend() {
    }

    private EmailSend(Builder builder) {
        this.sendId = builder.sendId;
        this.email = builder.email;
        this.flow = builder.flow;
        this.step = builder.step;
        this.contextRef = builder.contextRef;
        this.sentAt = builder.sentAt;
        this.openedAt = builder.openedAt;
        this.clickedAt = builder.clickedAt;
    }

    public String getSendId() {
        return sendId;
    }

    public String getEmail() {
        return email;
    }

    public EmailFlow getFlow() {
        return flow;
    }

    public int getStep() {
        return step;
    }

    public String getContextRef() {
        return contextRef;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public LocalDateTime getClickedAt() {
        return clickedAt;
    }

    public static class Builder {
        private String sendId;
        private String email;
        private EmailFlow flow;
        private int step;
        private String contextRef;
        private LocalDateTime sentAt;
        private LocalDateTime openedAt;
        private LocalDateTime clickedAt;

        public Builder setSendId(String sendId) {
            this.sendId = sendId;
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

        public Builder setStep(int step) {
            this.step = step;
            return this;
        }

        public Builder setContextRef(String contextRef) {
            this.contextRef = contextRef;
            return this;
        }

        public Builder setSentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        public Builder setOpenedAt(LocalDateTime openedAt) {
            this.openedAt = openedAt;
            return this;
        }

        public Builder setClickedAt(LocalDateTime clickedAt) {
            this.clickedAt = clickedAt;
            return this;
        }

        public Builder copy(EmailSend emailSend) {
            this.sendId = emailSend.sendId;
            this.email = emailSend.email;
            this.flow = emailSend.flow;
            this.step = emailSend.step;
            this.contextRef = emailSend.contextRef;
            this.sentAt = emailSend.sentAt;
            this.openedAt = emailSend.openedAt;
            this.clickedAt = emailSend.clickedAt;
            return this;
        }

        public EmailSend build() {
            return new EmailSend(this);
        }
    }
}
