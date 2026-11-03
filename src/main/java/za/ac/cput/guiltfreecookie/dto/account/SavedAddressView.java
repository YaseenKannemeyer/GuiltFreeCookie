package za.ac.cput.guiltfreecookie.dto.account;

import za.ac.cput.guiltfreecookie.domain.Address;

import java.time.LocalDateTime;

public record SavedAddressView(
        String id,
        String label,
        String recipientName,
        String phone,
        Boolean isDefault,
        LocalDateTime createdAt,
        Address address) {
}
