package za.ac.cput.guiltfreecookie.factory;

import za.ac.cput.guiltfreecookie.domain.Address;
import za.ac.cput.guiltfreecookie.domain.DeliveryDetails;
import za.ac.cput.guiltfreecookie.util.Helper;

public class DeliveryDetailsFactory {

    public static DeliveryDetails createDeliveryDetails(
            String deliveryID,
            Address address) {

        if (Helper.isNullOrEmpty(deliveryID)
                || address == null) {
            return null;
        }

        return new DeliveryDetails.Builder()
                .setDeliveryID(deliveryID)
                .setAddress(address)
                .build();
    }
}

