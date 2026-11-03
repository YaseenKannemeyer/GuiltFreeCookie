package za.ac.cput.guiltfreecookie.dto.analytics;

import za.ac.cput.guiltfreecookie.domain.EmailFlow;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Paid orders only (refunded ones are counted separately). Dates are shop days (Africa/Johannesburg).
public record AnalyticsSummary(
        LocalDate from,
        LocalDate to,
        Totals current,
        Totals previous,
        List<Day> days,
        List<TopCookie> topCookies,
        Customers customers,
        Funnel funnel,
        List<DeliveryShare> delivery,
        List<FlowResult> emailFlows) {

    public record Totals(BigDecimal revenue, long orders, BigDecimal averageOrderValue, long cookiesSold,
                         BigDecimal discounts, long refunds) {
    }

    public record Day(LocalDate date, BigDecimal revenue, long orders) {
    }

    public record TopCookie(String cookieId, String name, long packs, long cookies, BigDecimal revenue) {
    }

    public record Customers(long newCustomers, long returningCustomers) {
    }

    // Carts started in the period; carts that went to pay (retries count once); those orders now paid (refunds excluded).
    public record Funnel(long carts, long cartsWithEmail, long ordersPlaced, long ordersPaid) {
    }

    public record DeliveryShare(ShippingMethod method, long orders) {
    }

    // Revenue from paid orders placed within five days of a click on that flow's email.
    public record FlowResult(EmailFlow flow, long sent, long opened, long clicked, long orders, BigDecimal revenue) {
    }
}
