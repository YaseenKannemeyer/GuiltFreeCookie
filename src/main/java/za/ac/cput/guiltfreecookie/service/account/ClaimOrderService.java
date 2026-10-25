package za.ac.cput.guiltfreecookie.service.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.CustomerOrder;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.account.AccountCustomerRepository;
import za.ac.cput.guiltfreecookie.repository.account.AccountOrderRepository;
import za.ac.cput.guiltfreecookie.service.notification.EmailService;

import java.util.List;
import java.util.Map;

// Adds earlier paid guest orders to an account. There's no email verification at sign-up, so the
// customer proves they own the inbox by clicking a signed link sent to it.
@Service
public class ClaimOrderService {

    private final AccountOrderRepository orderRepository;
    private final AccountCustomerRepository customerRepository;
    private final ClaimOrderLinks claimLinks;
    private final EmailService emailService;

    public ClaimOrderService(AccountOrderRepository orderRepository, AccountCustomerRepository customerRepository,
                             ClaimOrderLinks claimLinks, EmailService emailService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.claimLinks = claimLinks;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public long count(String email) {
        return orderRepository.countClaimable(email);
    }

    public void request(String email) {
        if (orderRepository.countClaimable(email) == 0) {
            return;
        }
        emailService.sendTemplate(email, "Add your earlier GuiltFreeCookie orders", "claim-orders",
                Map.of("actionUrl", claimLinks.link(email), "actionLabel", "Add my earlier orders"), false);
    }

    @Transactional
    public long confirm(String email, String token) {
        claimLinks.verify(token, email);
        customerRepository.lock(email).orElseThrow(() -> new NotFoundException("Account not found"));
        List<CustomerOrder> eligible = orderRepository.lockClaimable(email);
        for (CustomerOrder order : eligible) {
            orderRepository.save(new CustomerOrder.Builder().copy(order).setAccountOwned(true).build());
        }
        return eligible.size();
    }
}
