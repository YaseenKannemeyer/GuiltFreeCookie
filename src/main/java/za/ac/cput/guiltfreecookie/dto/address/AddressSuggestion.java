package za.ac.cput.guiltfreecookie.dto.address;

// mainText is the street line, e.g. "10 Jan Smuts Avenue"; secondaryText the area, e.g. "Parktown, Johannesburg".
public record AddressSuggestion(String placeId, String mainText, String secondaryText) {
}
