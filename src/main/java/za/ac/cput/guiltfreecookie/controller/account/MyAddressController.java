package za.ac.cput.guiltfreecookie.controller.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.guiltfreecookie.dto.account.SavedAddressRequest;
import za.ac.cput.guiltfreecookie.dto.account.SavedAddressView;
import za.ac.cput.guiltfreecookie.service.account.AddressBookService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/addresses")
@Tag(name = "My addresses", description = "Up to ten saved addresses, with one default")
public class MyAddressController {

    private final AddressBookService addresses;

    public MyAddressController(AddressBookService addresses) {
        this.addresses = addresses;
    }

    @GetMapping
    @Operation(summary = "List my saved addresses, newest first")
    public List<SavedAddressView> list(@AuthenticationPrincipal String email) {
        return addresses.list(email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Save an address; the first becomes the default")
    public SavedAddressView create(@AuthenticationPrincipal String email, @RequestBody SavedAddressRequest request) {
        return addresses.create(email, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update my saved address")
    public SavedAddressView update(
        @AuthenticationPrincipal String email,
        @PathVariable String id,
        @RequestBody SavedAddressRequest request
    ) {
        return addresses.update(email, id, request);
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "Make this my default address")
    public SavedAddressView makeDefault(@AuthenticationPrincipal String email, @PathVariable String id) {
        return addresses.makeDefault(email, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete my address, promoting the newest remaining default if needed")
    public void delete(@AuthenticationPrincipal String email, @PathVariable String id) {
        addresses.delete(email, id);
    }
}
