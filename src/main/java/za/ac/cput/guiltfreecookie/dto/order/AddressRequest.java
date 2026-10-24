package za.ac.cput.guiltfreecookie.dto.order;

import za.ac.cput.guiltfreecookie.domain.Province;

public record AddressRequest(
        String streetNumber,
        String streetName,
        String complex,
        String suburb,
        String city,
        Province province,
        String postalCode) {
}
