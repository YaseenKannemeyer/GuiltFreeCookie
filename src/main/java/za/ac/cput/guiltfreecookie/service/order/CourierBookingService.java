package za.ac.cput.guiltfreecookie.service.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Address;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.OrderStatus;
import za.ac.cput.guiltfreecookie.domain.ShippingMethod;
import za.ac.cput.guiltfreecookie.dto.order.AdminOrderView;
import za.ac.cput.guiltfreecookie.dto.order.StatusChangeRequest;
import za.ac.cput.guiltfreecookie.dto.order.TrackingRequest;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CustomerOrderRepository;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;
import za.ac.cput.guiltfreecookie.service.shipping.DeliveryArea;
import za.ac.cput.guiltfreecookie.service.shipping.ShipLogicClient;
import za.ac.cput.guiltfreecookie.service.shipping.ShipLogicException;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

// Books The Courier Guy for a paid courier order, fetches its waybill, and marks it delivered when the courier says so.
@Service
public class CourierBookingService {

    public static final String COURIER = "The Courier Guy";
    static final String DELIVERED = "delivered";
    private static final Logger log = LoggerFactory.getLogger(CourierBookingService.class);
    private static final ZoneId SHOP_TIME = ZoneId.of("Africa/Johannesburg");

    private final OrderService orderService;
    private final AdminOrderService adminOrderService;
    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final ShipLogicClient shipLogic;
    private final Set<DayOfWeek> bookingDays;
    private final Clock clock;

    @Autowired
    public CourierBookingService(OrderService orderService, AdminOrderService adminOrderService,
                                 CustomerOrderRepository orderRepository, OrderItemRepository itemRepository,
                                 ShipLogicClient shipLogic,
                                 @Value("${app.shiplogic.booking-days:MONDAY,TUESDAY,WEDNESDAY}") String bookingDays) {
        this(orderService, adminOrderService, orderRepository, itemRepository, shipLogic, days(bookingDays), Clock.system(SHOP_TIME));
    }

    CourierBookingService(OrderService orderService, AdminOrderService adminOrderService, CustomerOrderRepository orderRepository,
                          OrderItemRepository itemRepository, ShipLogicClient shipLogic, Set<DayOfWeek> bookingDays, Clock clock) {
        this.orderService = orderService;
        this.adminOrderService = adminOrderService;
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.shipLogic = shipLogic;
        this.bookingDays = bookingDays;
        this.clock = clock;
    }

    // Booking and the tracking update happen together: a failed booking leaves the order as it was.
    @Transactional
    public AdminOrderView book(String orderNumber, String adminId) {
        CustomerOrder order = orderService.lock(orderNumber);
        if (!shipLogic.enabled()) {
            throw new ConflictException("Courier booking isn't set up. Book in the ShipLogic dashboard and enter the tracking number.");
        }
        if (order.getShippingMethod() != ShippingMethod.COURIER) {
            throw new ConflictException("Only courier orders can be booked with " + COURIER);
        }
        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new ConflictException("Already shipped with " + order.getCourierName() + " " + order.getTrackingNumber());
        }
        if (!EnumSet.of(OrderStatus.PAID, OrderStatus.PROCESSING).contains(order.getStatus())) {
            throw new ConflictException("Only paid orders can be booked");
        }
        DayOfWeek today = LocalDate.now(clock).getDayOfWeek();
        if (!bookingDays.contains(today)) {
            throw new ConflictException("Couriers are only booked on " + dayNames()
                    + ", so the cookies don't wait in a depot over the weekend. Book it then, or enter tracking by hand.");
        }
        Address address = order.getAddress();
        DeliveryArea area = DeliveryArea.of(address);
        if (area == null || order.getPhone() == null) {
            throw new ConflictException("This order's address or phone number is incomplete");
        }
        String street = (address.getComplex() == null ? "" : address.getComplex() + ", ")
                + address.getStreetNumber() + " " + address.getStreetName();
        ShipLogicClient.Booking booking;
        try {
            booking = shipLogic.book(order.getOrderNumber(),
                    new ShipLogicClient.Recipient(order.getFirstName() + " " + order.getLastName(), order.getPhone(),
                            order.getCustomerEmail(), street, area, order.getCustomerNote()),
                    cookieCount(order), order.getSubtotal().subtract(order.getDiscountTotal()));
        } catch (ShipLogicException e) {
            throw new ConflictException(e.getMessage());
        }
        log.info("Booked {} for {} at R{}", booking.trackingReference(), orderNumber, booking.rate());
        return adminOrderService.ship(orderNumber, new TrackingRequest(COURIER, booking.trackingReference(),
                shipLogic.trackingUrl(booking.trackingReference())), adminId);
    }

    @Transactional(readOnly = true)
    public String waybillUrl(String orderNumber) {
        CustomerOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException(OrderService.NOT_FOUND));
        if (!COURIER.equals(order.getCourierName()) || order.getTrackingNumber() == null) {
            throw new ConflictException("This order wasn't booked with " + COURIER);
        }
        try {
            return shipLogic.waybillUrl(order.getTrackingNumber());
        } catch (ShipLogicException e) {
            throw new ConflictException(e.getMessage());
        }
    }

    // ShipLogic's webhook format isn't documented, so shipped parcels are checked on a timer instead.
    @Scheduled(fixedDelayString = "${app.shiplogic.tracking-check-ms:1800000}", initialDelayString = "${app.shiplogic.tracking-check-ms:1800000}")
    public void checkDeliveries() {
        if (!shipLogic.enabled()) {
            return;
        }
        for (CustomerOrder order : orderRepository.findByStatusAndCourierName(OrderStatus.SHIPPED, COURIER)) {
            if (order.getTrackingNumber() == null) {
                continue;
            }
            if (shipLogic.status(order.getTrackingNumber()).filter(DELIVERED::equals).isPresent()) {
                try {
                    adminOrderService.changeStatus(order.getOrderNumber(),
                            new StatusChangeRequest(OrderStatus.DELIVERED, COURIER + " tracking shows it delivered"), "courier");
                } catch (RuntimeException e) {
                    log.warn("Couldn't mark {} delivered: {}", order.getOrderNumber(), e.toString());
                }
            }
        }
    }

    public boolean enabled() {
        return shipLogic.enabled();
    }

    private int cookieCount(CustomerOrder order) {
        return itemRepository.findByOrderId(order.getOrderId()).stream()
                .mapToInt(item -> item.getQuantity() * item.getUnitsPerPack()).sum();
    }

    private String dayNames() {
        return bookingDays.stream().sorted()
                .map(day -> day.getDisplayName(TextStyle.FULL, Locale.ENGLISH)).collect(Collectors.joining(", "));
    }

    static Set<DayOfWeek> days(String names) {
        return Arrays.stream(names.split(",")).map(String::trim).filter(name -> !name.isEmpty())
                .map(name -> DayOfWeek.valueOf(name.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }
}
