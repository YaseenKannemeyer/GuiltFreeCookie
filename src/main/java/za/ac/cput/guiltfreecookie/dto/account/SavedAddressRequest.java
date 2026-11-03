package za.ac.cput.guiltfreecookie.dto.account;

import za.ac.cput.guiltfreecookie.dto.order.AddressRequest;

public record SavedAddressRequest(String label, String recipientName, String phone, Boolean isDefault, AddressRequest address) {
}
