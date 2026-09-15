package za.ac.cput.guiltfreecookie.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import za.ac.cput.guiltfreecookie.domain.Address;
import za.ac.cput.guiltfreecookie.domain.DeliveryDetails;
import za.ac.cput.guiltfreecookie.factory.AddressFactory;
import za.ac.cput.guiltfreecookie.factory.DeliveryDetailsFactory;
import za.ac.cput.guiltfreecookie.service.DeliveryDetailsService;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeliveryDetailsControllerTest {

    @Mock
    private DeliveryDetailsService service;

    @InjectMocks
    private DeliveryDetailsController controller;

    private DeliveryDetails deliveryDetails;
    private Address address;

    @BeforeEach
    void setUp() {
        address = AddressFactory.createAddress(
                "15",
                "Main Road",
                "Cape Town",
                8001L
        );

        deliveryDetails = DeliveryDetailsFactory.createDeliveryDetails(
                "DEL001",
                address
        );
    }

    @Test
    void createDeliveryDetails() {
        when(service.create(any(DeliveryDetails.class)))
                .thenReturn(deliveryDetails);

        ResponseEntity<DeliveryDetails> response =
                controller.createDeliveryDetails(deliveryDetails);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(
                "DEL001",
                response.getBody().getDeliveryID()
        );

        verify(service).create(deliveryDetails);
    }

    @Test
    void getAllDeliveryDetails() {
        when(service.getAll())
                .thenReturn(Arrays.asList(deliveryDetails));

        ResponseEntity<List<DeliveryDetails>> response =
                controller.getAll();

        assertNotNull(response);
        assertNotNull(response.getBody());

        verify(service).getAll();
    }

    @Test
    void readDeliveryDetails() {
        when(service.read("DEL001"))
                .thenReturn(deliveryDetails);

        ResponseEntity<DeliveryDetails> response =
                controller.readDeliveryDetails("DEL001");

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(
                "DEL001",
                response.getBody().getDeliveryID()
        );

        verify(service).read("DEL001");
    }

    @Test
    void updateDeliveryDetails() {
        when(service.update(any(DeliveryDetails.class)))
                .thenReturn(deliveryDetails);

        ResponseEntity<DeliveryDetails> response =
                controller.updateDeliveryDetails(deliveryDetails);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(
                "DEL001",
                response.getBody().getDeliveryID()
        );

        verify(service).update(deliveryDetails);
    }

    @Test
    void deleteDeliveryDetails() {
        when(service.delete("DEL001"))
                .thenReturn(true);

        ResponseEntity<Boolean> response =
                controller.deleteDeliveryDetails("DEL001");

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody());

        verify(service).delete("DEL001");
    }
}