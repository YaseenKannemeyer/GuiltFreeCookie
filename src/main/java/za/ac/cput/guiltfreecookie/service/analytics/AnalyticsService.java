package za.ac.cput.guiltfreecookie.service.analytics;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.EmailFlow;
import za.ac.cput.guiltfreecookie.domain.EmailSend;
import za.ac.cput.guiltfreecookie.domain.OrderItem;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.dto.analytics.AnalyticsSummary;
import za.ac.cput.guiltfreecookie.dto.analytics.AnalyticsSummary.Customers;
import za.ac.cput.guiltfreecookie.dto.analytics.AnalyticsSummary.Day;
import za.ac.cput.guiltfreecookie.dto.analytics.AnalyticsSummary.DeliveryShare;
import za.ac.cput.guiltfreecookie.dto.analytics.AnalyticsSummary.FlowResult;
import za.ac.cput.guiltfreecookie.dto.analytics.AnalyticsSummary.Funnel;
import za.ac.cput.guiltfreecookie.dto.analytics.AnalyticsSummary.TopCookie;
import za.ac.cput.guiltfreecookie.dto.analytics.AnalyticsSummary.Totals;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.EmailSendRepository;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;
import za.ac.cput.guiltfreecookie.service.flow.FlowEnrollmentService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The admin overview. Small-shop scale: one period's paid orders are loaded and summed in memory.
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    static final int MAX_DAYS = 366;
    static final int TOP_COOKIES = 5;
    // An order counts towards an email flow if it was paid within this long after a click.
    static final Duration ATTRIBUTION_WINDOW = Duration.ofDays(5);

    private static final ZoneId SHOP = FlowEnrollmentService.SHOP_ZONE;

    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final CartRepository cartRepository;
    private final EmailSendRepository sendRepository;

    public AnalyticsService(CustomerOrderRepository orderRepository, OrderItemRepository itemRepository,
                            CartRepository cartRepository, EmailSendRepository sendRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.cartRepository = cartRepository;
        this.sendRepository = sendRepository;
    }

    // from and to are inclusive shop days. The previous period is the same number of days just before.
    public AnalyticsSummary summary(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException("Choose a start date on or before the end date");
        }
        long length = ChronoUnit.DAYS.between(from, to) + 1;
        if (length > MAX_DAYS) {
            throw new BadRequestException("Choose a period of at most " + MAX_DAYS + " days");
        }
        LocalDateTime start = startOf(from);
        LocalDateTime end = startOf(to.plusDays(1));
        LocalDateTime previousStart = startOf(from.minusDays(length));

        List<CustomerOrder> paid = paidOrders(start, end);
        List<OrderItem> items = paid.isEmpty() ? List.of()
                : itemRepository.findByOrderIdIn(paid.stream().map(CustomerOrder::getOrderId).toList());

        return new AnalyticsSummary(
                from,
                to,
                totals(paid, items, start, end),
                totals(paidOrders(previousStart, start), null, previousStart, start),
                days(paid, from, to),
                topCookies(items),
                customers(paid, start),
                new Funnel(
                        cartRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end),
                        cartRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndCustomerEmailIsNotNull(start, end),
                        orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end),
                        orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndPaidAtIsNotNull(start, end)),
                delivery(paid),
                emailFlows(paid, start, end));
    }

    private List<CustomerOrder> paidOrders(LocalDateTime from, LocalDateTime to) {
        return orderRepository.findByStatusInAndPaidAtGreaterThanEqualAndPaidAtLessThan(OrderStatus.PAID_STATUSES, from, to);
    }

    // The current period passes its already-loaded lines; for the previous one they're loaded here.
    private Totals totals(List<CustomerOrder> orders, List<OrderItem> items, LocalDateTime from, LocalDateTime to) {
        BigDecimal revenue = sum(orders.stream().map(CustomerOrder::getTotal).toList());
        BigDecimal discounts = sum(orders.stream().map(CustomerOrder::getDiscountTotal).toList());
        List<OrderItem> lines = items != null ? items : orders.isEmpty() ? List.of()
                : itemRepository.findByOrderIdIn(orders.stream().map(CustomerOrder::getOrderId).toList());
        long cookies = lines.stream().mapToLong(line -> (long) line.getQuantity() * line.getUnitsPerPack()).sum();
        BigDecimal average = orders.isEmpty() ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);
        long refunds = orderRepository.countByStatusAndPaidAtGreaterThanEqualAndPaidAtLessThan(OrderStatus.REFUNDED, from, to);
        return new Totals(revenue, orders.size(), average, cookies, discounts, refunds);
    }

    // Every day of the period, including the quiet ones.
    private List<Day> days(List<CustomerOrder> orders, LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> revenue = new HashMap<>();
        Map<LocalDate, Long> count = new HashMap<>();
        for (CustomerOrder order : orders) {
            LocalDate day = shopDay(order.getPaidAt());
            revenue.merge(day, order.getTotal(), BigDecimal::add);
            count.merge(day, 1L, Long::sum);
        }
        List<Day> days = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            days.add(new Day(day, revenue.getOrDefault(day, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                    count.getOrDefault(day, 0L)));
        }
        return days;
    }

    private List<TopCookie> topCookies(List<OrderItem> items) {
        Map<String, TopCookie> byCookie = new LinkedHashMap<>();
        for (OrderItem item : items) {
            TopCookie current = byCookie.getOrDefault(item.getCookieId(),
                    new TopCookie(item.getCookieId(), item.getCookieName(), 0, 0, BigDecimal.ZERO));
            byCookie.put(item.getCookieId(), new TopCookie(current.cookieId(), current.name(),
                    current.packs() + item.getQuantity(),
                    current.cookies() + (long) item.getQuantity() * item.getUnitsPerPack(),
                    current.revenue().add(item.getLineTotal())));
        }
        return byCookie.values().stream()
                .sorted(Comparator.comparing(TopCookie::revenue).reversed().thenComparing(TopCookie::name))
                .limit(TOP_COOKIES)
                .toList();
    }

    // Returning means they'd paid for an order before this period started.
    private Customers customers(List<CustomerOrder> orders, LocalDateTime start) {
        Set<String> emails = new HashSet<>();
        for (CustomerOrder order : orders) {
            emails.add(order.getCustomerEmail());
        }
        if (emails.isEmpty()) {
            return new Customers(0, 0);
        }
        long returning = orderRepository.emailsPaidBefore(emails, OrderStatus.PAID_STATUSES, start).size();
        return new Customers(emails.size() - returning, returning);
    }

    private List<DeliveryShare> delivery(List<CustomerOrder> orders) {
        Map<ShippingMethod, Long> byMethod = new EnumMap<>(ShippingMethod.class);
        for (CustomerOrder order : orders) {
            byMethod.merge(order.getShippingMethod(), 1L, Long::sum);
        }
        return byMethod.entrySet().stream().map(entry -> new DeliveryShare(entry.getKey(), entry.getValue())).toList();
    }

    // Each paid order goes to the flow of the latest click before it, if that click was within the window.
    private List<FlowResult> emailFlows(List<CustomerOrder> orders, LocalDateTime start, LocalDateTime end) {
        Map<EmailFlow, long[]> counts = new EnumMap<>(EmailFlow.class);
        for (Object[] row : sendRepository.statsBetween(start, end)) {
            counts.put((EmailFlow) row[0], new long[]{(Long) row[1], (Long) row[2], (Long) row[3]});
        }
        Map<String, List<EmailSend>> clicksByEmail = new HashMap<>();
        for (EmailSend send : sendRepository.findByClickedAtGreaterThanEqualAndClickedAtLessThan(
                start.minus(ATTRIBUTION_WINDOW), end)) {
            clicksByEmail.computeIfAbsent(send.getEmail(), ignored -> new ArrayList<>()).add(send);
        }
        Map<EmailFlow, Long> attributedOrders = new EnumMap<>(EmailFlow.class);
        Map<EmailFlow, BigDecimal> attributedRevenue = new EnumMap<>(EmailFlow.class);
        for (CustomerOrder order : orders) {
            clicksByEmail.getOrDefault(order.getCustomerEmail(), List.of()).stream()
                    .filter(click -> !click.getClickedAt().isAfter(order.getPaidAt())
                            && !click.getClickedAt().isBefore(order.getPaidAt().minus(ATTRIBUTION_WINDOW)))
                    .max(Comparator.comparing(EmailSend::getClickedAt))
                    .ifPresent(click -> {
                        attributedOrders.merge(click.getFlow(), 1L, Long::sum);
                        attributedRevenue.merge(click.getFlow(), order.getTotal(), BigDecimal::add);
                    });
        }
        List<FlowResult> results = new ArrayList<>();
        for (EmailFlow flow : EmailFlow.values()) {
            long[] sent = counts.getOrDefault(flow, new long[3]);
            results.add(new FlowResult(flow, sent[0], sent[1], sent[2], attributedOrders.getOrDefault(flow, 0L),
                    attributedRevenue.getOrDefault(flow, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)));
        }
        return results;
    }

    private static BigDecimal sum(List<BigDecimal> amounts) {
        return amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    // Shop days start at midnight in Cape Town; stored times are in the server's zone.
    static LocalDateTime startOf(LocalDate day) {
        return day.atStartOfDay(SHOP).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    static LocalDate shopDay(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).withZoneSameInstant(SHOP).toLocalDate();
    }
}
