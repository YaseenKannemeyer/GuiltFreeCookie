package za.ac.cput.guiltfreecookie.service.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.domain.OrderItem;
import za.ac.cput.guiltfreecookie.dto.account.MyOrderDetail;
import za.ac.cput.guiltfreecookie.dto.account.MyOrderSummary;
import za.ac.cput.guiltfreecookie.dto.catalog.PageResponse;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.OrderItemRepository;
import za.ac.cput.guiltfreecookie.repository.account.AccountOrderRepository;
import za.ac.cput.guiltfreecookie.service.order.OrderLinks;
import za.ac.cput.guiltfreecookie.service.order.OrderViews;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MyOrderService {

    static final int DEFAULT_PAGE_SIZE = 10;
    static final int MAX_PAGE_SIZE = 100;

    private final AccountOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final CookieRepository cookieRepository;
    private final OrderLinks orderLinks;
    private final OrderViews orderViews;

    public MyOrderService(AccountOrderRepository orderRepository, OrderItemRepository itemRepository,
                          CookieRepository cookieRepository, OrderLinks orderLinks, OrderViews orderViews) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.cookieRepository = cookieRepository;
        this.orderLinks = orderLinks;
        this.orderViews = orderViews;
    }

    // Newest first. Each row carries the order page token, so the account page links to the existing tracking page.
    public PageResponse<MyOrderSummary> history(String email, Integer page, Integer size) {
        int number = page == null ? 0 : page;
        int count = size == null ? DEFAULT_PAGE_SIZE : size;
        if (number < 0 || count < 1 || count > MAX_PAGE_SIZE) {
            throw new BadRequestException("page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE);
        }
        Page<CustomerOrder> result = orderRepository.history(email, LocalDateTime.now(),
                PageRequest.of(number, count, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("orderId"))));
        if (result.isEmpty()) {
            return PageResponse.of(result, ignored -> List.of());
        }
        Map<String, List<OrderItem>> lines = itemRepository
                .findByOrderIdIn(result.getContent().stream().map(CustomerOrder::getOrderId).toList())
                .stream()
                .sorted(Comparator.comparing(OrderItem::getOrderItemId))
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        Map<String, String> images = cookieRepository
                .findAllById(lines.values().stream().map(items -> items.getFirst().getCookieId()).distinct().toList())
                .stream()
                .filter(cookie -> cookie.getImage() != null)
                .collect(Collectors.toMap(Cookie::getCookieId, Cookie::getImage));
        return PageResponse.of(result, orders -> orders.stream().map(order -> {
            List<OrderItem> items = lines.getOrDefault(order.getOrderId(), List.of());
            String image = items.isEmpty() ? null : images.get(items.getFirst().getCookieId());
            return new MyOrderSummary(order.getOrderNumber(), order.getCreatedAt(), order.getStatus(),
                    order.getShippingMethod(), items.stream().mapToInt(OrderItem::getQuantity).sum(),
                    order.getTotal(), image, orderLinks.token(order));
        }).toList());
    }

    // Someone else's order gets the same 404 as a missing one.
    public MyOrderDetail get(String email, String orderNumber) {
        CustomerOrder order = orderRepository.findByOrderNumberAndCustomerEmailAndAccountOwnedTrue(orderNumber, email)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return new MyOrderDetail(orderViews.view(order), orderLinks.token(order));
    }
}
