package za.ac.cput.guiltfreecookie.service.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Address;
import za.ac.cput.guiltfreecookie.domain.DeliveryDetails;
import za.ac.cput.guiltfreecookie.dto.account.SavedAddressRequest;
import za.ac.cput.guiltfreecookie.dto.account.SavedAddressView;
import za.ac.cput.guiltfreecookie.dto.order.AddressRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.account.AccountCustomerRepository;
import za.ac.cput.guiltfreecookie.repository.account.SavedAddressRepository;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// A customer's saved addresses. Every change locks the customer row, so two quick saves can't pass the cap.
@Service
@Transactional
public class AddressBookService {

    static final int MAX_ADDRESSES = 10;
    static final int MAX_LABEL_LENGTH = 30;
    static final int MAX_NAME_LENGTH = 200;

    private final SavedAddressRepository addressRepository;
    private final AccountCustomerRepository customerRepository;

    public AddressBookService(SavedAddressRepository addressRepository, AccountCustomerRepository customerRepository) {
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<SavedAddressView> list(String email) {
        return all(email).stream().map(AddressBookService::view).toList();
    }

    // The first address becomes the default.
    public SavedAddressView create(String email, SavedAddressRequest request) {
        lock(email);
        List<DeliveryDetails> current = all(email);
        if (current.size() >= MAX_ADDRESSES) {
            throw new ConflictException("You can save up to " + MAX_ADDRESSES + " addresses. Remove one to add another.");
        }
        boolean makeDefault = current.isEmpty() || Boolean.TRUE.equals(request.isDefault());
        DeliveryDetails address = validated(request)
                .setDeliveryID(UUID.randomUUID().toString())
                .setCustomerEmail(email)
                .setCreatedAt(LocalDateTime.now())
                .setIsDefault(makeDefault)
                .build();
        if (makeDefault) {
            clearDefaults(current);
        }
        return view(addressRepository.save(address));
    }

    // A default stays the default until another address is chosen or it's deleted.
    public SavedAddressView update(String email, String id, SavedAddressRequest request) {
        lock(email);
        DeliveryDetails existing = owned(email, id);
        boolean makeDefault = existing.isDefault() || Boolean.TRUE.equals(request.isDefault());
        DeliveryDetails replacement = validated(request)
                .setDeliveryID(id)
                .setCustomerEmail(email)
                .setCreatedAt(existing.getCreatedAt())
                .setIsDefault(makeDefault)
                .build();
        if (makeDefault) {
            clearDefaults(all(email));
        }
        return view(addressRepository.save(replacement));
    }

    public SavedAddressView makeDefault(String email, String id) {
        lock(email);
        DeliveryDetails existing = owned(email, id);
        clearDefaults(all(email));
        return view(addressRepository.save(new DeliveryDetails.Builder().copy(existing).setIsDefault(true).build()));
    }

    // Deleting the default promotes the newest remaining address.
    public void delete(String email, String id) {
        lock(email);
        DeliveryDetails existing = owned(email, id);
        addressRepository.delete(existing);
        if (existing.isDefault()) {
            all(email).stream()
                    .filter(address -> !address.getDeliveryID().equals(id))
                    .findFirst()
                    .ifPresent(address -> addressRepository.save(
                            new DeliveryDetails.Builder().copy(address).setIsDefault(true).build()));
        }
    }

    public static SavedAddressView view(DeliveryDetails address) {
        return new SavedAddressView(address.getDeliveryID(), address.getLabel(), address.getRecipientName(),
                address.getPhone(), address.isDefault(), address.getCreatedAt(), address.getAddress());
    }

    private void lock(String email) {
        customerRepository.lock(email).orElseThrow(() -> new NotFoundException("Account not found"));
    }

    private List<DeliveryDetails> all(String email) {
        return addressRepository.findByCustomerEmailOrderByCreatedAtDescDeliveryIDDesc(email);
    }

    // Someone else's address gets the same 404 as a missing one.
    private DeliveryDetails owned(String email, String id) {
        return addressRepository.findByDeliveryIDAndCustomerEmail(id, email)
                .orElseThrow(() -> new NotFoundException("Address not found"));
    }

    private void clearDefaults(List<DeliveryDetails> addresses) {
        for (DeliveryDetails address : addresses) {
            if (address.isDefault()) {
                addressRepository.save(new DeliveryDetails.Builder().copy(address).setIsDefault(false).build());
            }
        }
    }

    private DeliveryDetails.Builder validated(SavedAddressRequest request) {
        String label = Helper.clean(request.label());
        String recipient = Helper.clean(request.recipientName());
        String phone = Helper.clean(request.phone());
        if (!Helper.isPresent(label, MAX_LABEL_LENGTH)) {
            throw new BadRequestException("Enter an address label of up to " + MAX_LABEL_LENGTH + " characters");
        }
        if (!Helper.isPresent(recipient, MAX_NAME_LENGTH)) {
            throw new BadRequestException("Enter the recipient's name (up to " + MAX_NAME_LENGTH + " characters)");
        }
        if (!Helper.isPresent(phone, 30) || !Helper.isValidMobileNumber(phone)) {
            throw new BadRequestException("Enter a valid South African mobile number");
        }
        AddressRequest address = request.address();
        if (address == null) {
            throw new BadRequestException("Enter a delivery address");
        }
        String number = Helper.clean(address.streetNumber());
        String street = Helper.clean(address.streetName());
        String complex = Helper.clean(address.complex());
        String suburb = Helper.clean(address.suburb());
        String city = Helper.clean(address.city());
        String postalCode = Helper.clean(address.postalCode());
        if (!Helper.isPresent(number, 20) || !Helper.isPresent(street, 100) || !Helper.isWithin(complex, 100)
                || !Helper.isPresent(suburb, 100) || !Helper.isPresent(city, 100) || address.province() == null
                || postalCode == null || !postalCode.matches("[0-9]{4}")) {
            throw new BadRequestException("Enter the street, suburb, city, province and 4-digit postal code");
        }
        return new DeliveryDetails.Builder()
                .setLabel(label)
                .setRecipientName(recipient)
                .setPhone(phone)
                .setAddress(new Address.Builder()
                        .setStreetNumber(number)
                        .setStreetName(street)
                        .setComplex(complex)
                        .setSuburb(suburb)
                        .setCity(city)
                        .setProvince(address.province())
                        .setPostalCode(postalCode)
                        .build());
    }
}
