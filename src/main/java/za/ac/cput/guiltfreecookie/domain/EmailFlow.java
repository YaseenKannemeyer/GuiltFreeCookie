package za.ac.cput.guiltfreecookie.domain;

// The automated email sequences. Steps and copy live in service/flow/FlowCatalog.
public enum EmailFlow {
    WELCOME,
    BROWSE_ABANDONMENT,
    CART_ABANDONMENT,
    POST_PURCHASE,
    REPLENISHMENT,
    WINBACK,
    SUNSET
}
