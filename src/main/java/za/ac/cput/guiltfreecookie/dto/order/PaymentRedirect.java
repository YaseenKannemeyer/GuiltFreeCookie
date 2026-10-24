package za.ac.cput.guiltfreecookie.dto.order;

import java.util.Map;

// Build a <form method="POST" action="actionUrl"> with one hidden input per field, in order, and submit it.
public record PaymentRedirect(String gateway, String actionUrl, Map<String, String> fields) {
}
