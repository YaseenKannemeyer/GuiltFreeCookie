package za.ac.cput.guiltfreecookie.dto.auth;

import za.ac.cput.guiltfreecookie.domain.Customer;

public record CustomerAuthResponse(Customer customer, String token) {
}
