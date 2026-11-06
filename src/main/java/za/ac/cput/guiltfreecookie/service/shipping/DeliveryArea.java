package za.ac.cput.guiltfreecookie.service.shipping;

import za.ac.cput.guiltfreecookie.domain.Address;
import za.ac.cput.guiltfreecookie.domain.Province;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.util.Locale;

// Where a courier delivery goes, down to the suburb. Couriers price by area, so the street never leaves the shop.
public record DeliveryArea(String suburb, String city, Province province, String postalCode) {

    static final int MAX_PART_LENGTH = 100;

    // Null unless every part is present and the postal code is valid.
    public static DeliveryArea of(String suburb, String city, Province province, String postalCode) {
        String cleanSuburb = Helper.clean(suburb);
        String cleanCity = Helper.clean(city);
        String cleanCode = Helper.clean(postalCode);
        if (!Helper.isPresent(cleanSuburb, MAX_PART_LENGTH) || !Helper.isPresent(cleanCity, MAX_PART_LENGTH)
                || province == null || !Helper.isValidPostalCode(cleanCode)) {
            return null;
        }
        return new DeliveryArea(cleanSuburb, cleanCity, province, cleanCode);
    }

    public static DeliveryArea of(Address address) {
        return address == null ? null
                : of(address.getSuburb(), address.getCity(), address.getProvince(), address.getPostalCode());
    }

    String cacheKey() {
        return String.join("|", suburb.toLowerCase(Locale.ROOT), city.toLowerCase(Locale.ROOT), province.name(), postalCode);
    }
}
