package za.ac.cput.guiltfreecookie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.ac.cput.guiltfreecookie.dto.address.AddressSearchResult;
import za.ac.cput.guiltfreecookie.dto.order.AddressRequest;
import za.ac.cput.guiltfreecookie.service.address.AddressSearchService;

@RestController
@RequestMapping("/api/v1/address-search")
@Tag(name = "Address search", description = "Suggests South African addresses as the customer types at checkout.")
public class AddressSearchController {

    private final AddressSearchService addressSearchService;

    public AddressSearchController(AddressSearchService addressSearchService) {
        this.addressSearchService = addressSearchService;
    }

    @GetMapping
    @Operation(summary = "Suggest addresses", description = "Under 3 characters returns no suggestions, so an empty q just says whether search is enabled. "
            + "Send the same session (any id up to 64 letters, digits and dashes) with every keystroke and the chosen place.")
    public ResponseEntity<AddressSearchResult> suggest(@RequestParam(required = false) String q,
                                                       @RequestParam(required = false) String session) {
        return ResponseEntity.ok(addressSearchService.suggest(q, session));
    }

    @GetMapping("/place")
    @Operation(summary = "The chosen suggestion as address fields", description = "Fields Google doesn't know come back blank. 503 if the lookup fails.")
    public ResponseEntity<AddressRequest> place(@RequestParam String id, @RequestParam(required = false) String session) {
        return ResponseEntity.ok(addressSearchService.place(id, session));
    }
}
