package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// A message from the contact form, kept for the admin inbox.
@Entity
@Table(name = "contact_messages")
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 150)
    private String subject;

    @Column(nullable = false, length = 5000)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactStatus status;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // Admin id.
    @Column(name = "resolved_by")
    private String resolvedBy;

    protected ContactMessage() {
    }

    private ContactMessage(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.email = builder.email;
        this.phone = builder.phone;
        this.subject = builder.subject;
        this.message = builder.message;
        this.createdAt = builder.createdAt;
        this.status = builder.status;
        this.resolvedAt = builder.resolvedAt;
        this.resolvedBy = builder.resolvedBy;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ContactStatus getStatus() {
        return status;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public static class Builder {
        private String id;
        private String name;
        private String email;
        private String phone;
        private String subject;
        private String message;
        private LocalDateTime createdAt;
        private ContactStatus status;
        private LocalDateTime resolvedAt;
        private String resolvedBy;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder setSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder setStatus(ContactStatus status) {
            this.status = status;
            return this;
        }

        public Builder setResolvedAt(LocalDateTime resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        public Builder setResolvedBy(String resolvedBy) {
            this.resolvedBy = resolvedBy;
            return this;
        }

        public Builder copy(ContactMessage contactMessage) {
            this.id = contactMessage.id;
            this.name = contactMessage.name;
            this.email = contactMessage.email;
            this.phone = contactMessage.phone;
            this.subject = contactMessage.subject;
            this.message = contactMessage.message;
            this.createdAt = contactMessage.createdAt;
            this.status = contactMessage.status;
            this.resolvedAt = contactMessage.resolvedAt;
            this.resolvedBy = contactMessage.resolvedBy;
            return this;
        }

        public ContactMessage build() {
            return new ContactMessage(this);
        }
    }
}
