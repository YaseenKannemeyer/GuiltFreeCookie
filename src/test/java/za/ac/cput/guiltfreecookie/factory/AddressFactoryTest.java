package za.ac.cput.guiltfreecookie.factory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import za.ac.cput.guiltfreecookie.domain.Address;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AddressFactoryTest {

    @Test
    @Order(1)
    void testCreateAddressSuccess() {
        Address address = AddressFactory.createAddress(
                "15",
                "Main Road",
                "Cape Town",
                8001L
        );

        Assertions.assertNotNull(address);
        Assertions.assertEquals("15", address.getStreetNumber());
        Assertions.assertEquals("Main Road", address.getStreetName());
        Assertions.assertEquals("Cape Town", address.getCity());
        Assertions.assertEquals(8001L, address.getPostalCode());

        System.out.println(address);
    }

    @Test
    @Order(2)
    void testCreateAddressWithEmptyStreetName() {
        Address address = AddressFactory.createAddress(
                "15",
                "",
                "Cape Town",
                8001L
        );

        Assertions.assertNull(address);
        System.out.println("Empty street name test passed: " + address);
    }

    @Test
    @Order(3)
    void testCreateAddressWithInvalidPostalCode() {
        Address address = AddressFactory.createAddress(
                "15",
                "Main Road",
                "Cape Town",
                -1L
        );

        Assertions.assertNull(address);
        System.out.println("Invalid postal code test passed: " + address);
    }
}
