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
                "8001"
        );

        Assertions.assertNotNull(address);
        Assertions.assertEquals("15", address.getStreetNumber());
        Assertions.assertEquals("Main Road", address.getStreetName());
        Assertions.assertEquals("Cape Town", address.getCity());
        Assertions.assertEquals("8001", address.getPostalCode());

        System.out.println(address);
    }

    @Test
    @Order(2)
    void testCreateAddressWithEmptyStreetName() {
        Address address = AddressFactory.createAddress(
                "15",
                "",
                "Cape Town",
                "8001"
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
                "800"
        );

        Assertions.assertNull(address);
        Assertions.assertNull(AddressFactory.createAddress("15", "Main Road", "Cape Town", "80O1"));
        System.out.println("Invalid postal code test passed: " + address);
    }

    @Test
    @Order(4)
    void testPostalCodeKeepsItsLeadingZero() {
        Address address = AddressFactory.createAddress("1", "Church Street", "Pretoria", "0002");

        Assertions.assertNotNull(address);
        Assertions.assertEquals("0002", address.getPostalCode());
    }
}
