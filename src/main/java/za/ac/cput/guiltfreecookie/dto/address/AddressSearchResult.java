package za.ac.cput.guiltfreecookie.dto.address;

import java.util.List;

// enabled is false when no search provider is set up; failed means the provider didn't answer this time.
public record AddressSearchResult(boolean enabled, boolean failed, List<AddressSuggestion> suggestions) {
}
