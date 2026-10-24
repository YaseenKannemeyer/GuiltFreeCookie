package za.ac.cput.guiltfreecookie.dto.cart;

public enum LineIssue {
    // The cookie was archived or the pack size hidden: remove the line.
    UNAVAILABLE,
    // More than we have in stock: lower the quantity to maxQuantity or remove.
    NOT_ENOUGH_STOCK
}
