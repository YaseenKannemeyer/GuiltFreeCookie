package za.ac.cput.guiltfreecookie.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactStatus status;

    private LocalDateTime resolvedAt;

    private String resolvedBy;

    protected ContactMessage() {}

    private ContactMessage(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.email = b.email;
        this.phone = b.phone;
        this.subject = b.subject;
        this.message = b.message;
        this.createdAt = b.createdAt;
        this.status = b.status;
        this.resolvedAt = b.resolvedAt;
        this.resolvedBy = b.resolvedBy;
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

        public Builder setId(String value) {
            this.id = value;
            return this;
        }

        public Builder setName(String value) {
            this.name = value;
            return this;
        }

        public Builder setEmail(String value) {
            this.email = value;
            return this;
        }

        public Builder setPhone(String value) {
            this.phone = value;
            return this;
        }

        public Builder setSubject(String value) {
            this.subject = value;
            return this;
        }

        public Builder setMessage(String value) {
            this.message = value;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime value) {
            this.createdAt = value;
            return this;
        }

        public Builder setStatus(ContactStatus value) {
            this.status = value;
            return this;
        }

        public Builder setResolvedAt(LocalDateTime value) {
            this.resolvedAt = value;
            return this;
        }

        public Builder setResolvedBy(String value) {
            this.resolvedBy = value;
            return this;
        }

        public Builder copy(ContactMessage source) {
            this.id = source.id;
            this.name = source.name;
            this.email = source.email;
            this.phone = source.phone;
            this.subject = source.subject;
            this.message = source.message;
            this.createdAt = source.createdAt;
            this.status = source.status;
            this.resolvedAt = source.resolvedAt;
            this.resolvedBy = source.resolvedBy;
            return this;
        }

        public ContactMessage build() {
            return new ContactMessage(this);
        }
    }
}
