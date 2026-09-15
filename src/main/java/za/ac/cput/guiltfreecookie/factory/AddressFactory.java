package za.ac.cput.guiltfreecookie.factory;

import za.ac.cput.guiltfreecookie.domain.Address;
import za.ac.cput.guiltfreecookie.util.Helper;

public class AddressFactory {

    public static Address createAddress(
            String streetNumber,
            String streetName,
            String city,
            Long postalCode) {

        if (Helper.isNullOrEmpty(streetNumber)
                || Helper.isNullOrEmpty(streetName)
                || Helper.isNullOrEmpty(city)
                || postalCode == null
                || postalCode <= 0) {
            return null;
        }

        return new Address.Builder()
                .setStreetNumber(streetNumber)
                .setStreetName(streetName)
                .setCity(city)
                .setPostalCode(postalCode)
                .build();
    }
}