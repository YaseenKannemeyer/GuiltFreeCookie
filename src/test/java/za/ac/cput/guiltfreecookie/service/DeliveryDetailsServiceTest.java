package za.ac.cput.guiltfreecookie.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import za.ac.cput.guiltfreecookie.domain.Address;
import za.ac.cput.guiltfreecookie.domain.DeliveryDetails;
import za.ac.cput.guiltfreecookie.factory.AddressFactory;
import za.ac.cput.guiltfreecookie.factory.DeliveryDetailsFactory;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DeliveryDetailsServiceTest {

    @Autowired
    private DeliveryDetailsService service;

    private static DeliveryDetails deliveryDetails;

    @Test
    @Order(1)
    void create() {
        Address address = AddressFactory.createAddress(
                "15",
                "Main Road",
                "Cape Town",
                8001L
        );

        deliveryDetails = DeliveryDetailsFactory.createDeliveryDetails(
                "DEL001",
                address
        );

        DeliveryDetails created = service.create(deliveryDetails);

        Assertions.assertNotNull(created);
        Assertions.assertEquals("DEL001", created.getDeliveryID());

        System.out.println("Created: " + created);
    }

    @Test
    @Order(2)
    void read() {
        DeliveryDetails read = service.read("DEL001");

        Assertions.assertNotNull(read);
        Assertions.assertEquals("DEL001", read.getDeliveryID());

        System.out.println("Read: " + read);
    }

    @Test
    @Order(3)
    void update() {
        Address newAddress = AddressFactory.createAddress(
                "25",
                "Long Street",
                "Cape Town",
                8001L
        );

        DeliveryDetails updatedDetails =
                DeliveryDetailsFactory.createDeliveryDetails(
                        "DEL001",
                        newAddress
                );

        DeliveryDetails updated = service.update(updatedDetails);

        Assertions.assertNotNull(updated);
        Assertions.assertEquals(
                "Long Street",
                updated.getAddress().getStreetName()
        );

        System.out.println("Updated: " + updated);
    }

    @Test
    @Order(4)
    void getAll() {
        var all = service.getAll();

        Assertions.assertNotNull(all);
        Assertions.assertFalse(all.isEmpty());

        System.out.println("All delivery details: " + all);
    }

    @Test
    @Order(5)
    void delete() {
        boolean deleted = service.delete("DEL001");

        Assertions.assertTrue(deleted);
        Assertions.assertNull(service.read("DEL001"));

        System.out.println("Deleted successfully");
    }
}
