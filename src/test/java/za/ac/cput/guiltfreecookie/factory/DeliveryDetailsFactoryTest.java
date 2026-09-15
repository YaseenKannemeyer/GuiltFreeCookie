package za.ac.cput.guiltfreecookie.factory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import za.ac.cput.guiltfreecookie.domain.Address;
import za.ac.cput.guiltfreecookie.domain.DeliveryDetails;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DeliveryDetailsFactoryTest {

    @Test
    @Order(1)
    void testCreateDeliveryDetailsSuccess() {
        Address address = AddressFactory.createAddress(
                "12",
                "Nombula",
                "Cape Town",
                8001L
        );

        DeliveryDetails deliveryDetails =
                DeliveryDetailsFactory.createDeliveryDetails(
                        "DEL001",
                        address
                );

        Assertions.assertNotNull(deliveryDetails);
        Assertions.assertEquals("DEL001", deliveryDetails.getDeliveryID());
        Assertions.assertNotNull(deliveryDetails.getAddress());
        Assertions.assertEquals("12", deliveryDetails.getAddress().getStreetNumber());
        Assertions.assertEquals("Nombula", deliveryDetails.getAddress().getStreetName());

        System.out.println(deliveryDetails);
    }

    @Test
    @Order(2)
    void testCreateDeliveryDetailsWithEmptyDeliveryID() {
        Address address = AddressFactory.createAddress(
                "12",
                "Nombula",
                "Cape Town",
                8001L
        );

        DeliveryDetails deliveryDetails =
                DeliveryDetailsFactory.createDeliveryDetails(
                        "",
                        address
                );

        Assertions.assertNull(deliveryDetails);
        System.out.println("Empty delivery ID test passed: " + deliveryDetails);
    }

    @Test
    @Order(3)
    void testCreateDeliveryDetailsWithNullAddress() {
        DeliveryDetails deliveryDetails =
                DeliveryDetailsFactory.createDeliveryDetails(
                        "DEL003",
                        null
                );

        Assertions.assertNull(deliveryDetails);
        System.out.println("Null address test passed: " + deliveryDetails);
    }
}