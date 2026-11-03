package za.ac.cput.guiltfreecookie.service.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import za.ac.cput.guiltfreecookie.domain.*;
import za.ac.cput.guiltfreecookie.dto.account.*;
import za.ac.cput.guiltfreecookie.dto.order.AddressRequest;
import za.ac.cput.guiltfreecookie.exception.*;
import za.ac.cput.guiltfreecookie.repository.account.*;

class AddressBookServiceTest {

    SavedAddressRepository addresses = mock(SavedAddressRepository.class);
    AccountCustomerRepository customers = mock(AccountCustomerRepository.class);
    AddressBookService service = new AddressBookService(addresses, customers);
    String email = "sam@example.com";
    AddressRequest address = new AddressRequest(
        "12",
        " Main Road ",
        "Unit 4",
        "Central",
        "Pretoria",
        Province.GAUTENG,
        "0002"
    );
    SavedAddressRequest request = new SavedAddressRequest(" Home ", " Sam Baker ", "082 123 4567", false, address);

    DeliveryDetails existing(String id, boolean isDefault) {
        return new DeliveryDetails.Builder()
            .setDeliveryID(id)
            .setCustomerEmail(email)
            .setLabel("Home")
            .setRecipientName("Sam")
            .setPhone("0821234567")
            .setIsDefault(isDefault)
            .setCreatedAt(LocalDateTime.now())
            .build();
    }

    @BeforeEach
    void setup() {
        when(customers.lock(email)).thenReturn(Optional.of(new Customer.Builder().setCustomerEmail(email).build()));
        when(addresses.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void firstAddressBecomesDefaultAndPreservesPostalZero() {
        var result = service.create(email, request);
        assertTrue(result.isDefault());
        assertEquals("Home", result.label());
        assertEquals("Main Road", result.address().getStreetName());
        assertEquals("0002", result.address().getPostalCode());
        assertNotNull(result.createdAt());
        var order = inOrder(customers, addresses);
        order.verify(customers).lock(email);
        order.verify(addresses).findByCustomerEmailOrderByCreatedAtDescDeliveryIDDesc(email);
    }

    @Test
    void capIsTen() {
        when(addresses.findByCustomerEmailOrderByCreatedAtDescDeliveryIDDesc(email)).thenReturn(
            Collections.nCopies(10, existing("a", false))
        );
        assertThrows(ConflictException.class, () -> service.create(email, request));
        verify(addresses, never()).save(any());
    }

    @Test
    void otherOwnersAddressCannotBeUpdatedDeletedOrMadeDefault() {
        assertThrows(NotFoundException.class, () -> service.update(email, "other", request));
        assertThrows(NotFoundException.class, () -> service.delete(email, "other"));
        assertThrows(NotFoundException.class, () -> service.makeDefault(email, "other"));
        verify(addresses, never()).save(any());
        verify(addresses, never()).delete(any());
    }

    @Test
    void choosingDefaultClearsOldAndDeletingPromotesNewest() {
        var old = existing("old", true);
        var newest = existing("new", false);
        when(addresses.findByDeliveryIDAndCustomerEmail("new", email)).thenReturn(Optional.of(newest));
        when(addresses.findByCustomerEmailOrderByCreatedAtDescDeliveryIDDesc(email)).thenReturn(List.of(newest, old));
        assertTrue(service.makeDefault(email, "new").isDefault());
        verify(addresses).save(argThat(a -> a.getDeliveryID().equals("old") && !a.isDefault()));
        reset(addresses);
        when(addresses.save(any())).thenAnswer(i -> i.getArgument(0));
        when(addresses.findByDeliveryIDAndCustomerEmail("old", email)).thenReturn(Optional.of(old));
        when(addresses.findByCustomerEmailOrderByCreatedAtDescDeliveryIDDesc(email)).thenReturn(List.of(newest));
        service.delete(email, "old");
        verify(addresses).delete(old);
        verify(addresses).save(argThat(a -> a.getDeliveryID().equals("new") && a.isDefault()));
    }

    @Test
    void updatingDefaultPreservesItsIdentityAndCreationTime() {
        var old = existing("old", true);
        when(addresses.findByDeliveryIDAndCustomerEmail("old", email)).thenReturn(Optional.of(old));
        var updated = service.update(email, "old", request);
        assertEquals("old", updated.id());
        assertTrue(updated.isDefault());
        assertEquals(old.getCreatedAt(), updated.createdAt());
    }

    @Test
    void invalidNamesPhoneAndAddressAreRejected() {
        assertThrows(BadRequestException.class, () ->
            service.create(email, new SavedAddressRequest("x".repeat(31), "Sam", "0821234567", null, address))
        );
        assertThrows(BadRequestException.class, () ->
            service.create(email, new SavedAddressRequest("Home", "Sam", "123", null, address))
        );
        assertThrows(BadRequestException.class, () ->
            service.create(email, new SavedAddressRequest("Home", "Sam", "0821234567", null, null))
        );
        assertThrows(BadRequestException.class, () ->
            service.create(
                email,
                new SavedAddressRequest(
                    "Home",
                    "Sam",
                    "0821234567",
                    null,
                    new AddressRequest("12", "Street", null, "Suburb", "City", Province.GAUTENG, "2")
                )
            )
        );
        verify(addresses, never()).save(any());
    }

    @Test
    void deletingNonDefaultDoesNotChangeDefault() {
        var other = existing("other", false);
        when(addresses.findByDeliveryIDAndCustomerEmail("other", email)).thenReturn(Optional.of(other));
        service.delete(email, "other");
        verify(addresses, never()).save(any());
    }
}
