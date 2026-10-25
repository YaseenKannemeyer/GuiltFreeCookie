package za.ac.cput.guiltfreecookie.service.flow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.Cart;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.domain.CookieVariant;
import za.ac.cput.guiltfreecookie.domain.DiscountCode;
import za.ac.cput.guiltfreecookie.domain.EmailFlow;
import za.ac.cput.guiltfreecookie.domain.EmailSend;
import za.ac.cput.guiltfreecookie.domain.EnrollmentStatus;
import za.ac.cput.guiltfreecookie.domain.FlowEnrollment;
import za.ac.cput.guiltfreecookie.domain.NewsletterSubscriber;
import za.ac.cput.guiltfreecookie.dto.cart.AddItemRequest;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.repository.CookieVariantRepository;
import za.ac.cput.guiltfreecookie.repository.DiscountCodeRepository;
import za.ac.cput.guiltfreecookie.repository.EmailSendRepository;
import za.ac.cput.guiltfreecookie.repository.FlowEnrollmentRepository;
import za.ac.cput.guiltfreecookie.repository.NewsletterSubscriberRepository;
import za.ac.cput.guiltfreecookie.service.NewsletterService;
import za.ac.cput.guiltfreecookie.service.cart.ShopCartService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The flows end to end against the real database: enrol, send, issue codes, track, recover, unsubscribe.
@SpringBootTest
@Transactional
class FlowJourneyTest {

    @Autowired
    private NewsletterService newsletterService;
    @Autowired
    private FlowEnrollmentService enrollments;
    @Autowired
    private FlowRunner runner;
    @Autowired
    private FlowTriggers triggers;
    @Autowired
    private EmailLinkService linkService;
    @Autowired
    private EmailTrackingService trackingService;
    @Autowired
    private MarketingConsent consent;
    @Autowired
    private MarketingLinks links;
    @Autowired
    private ShopCartService cartService;
    @Autowired
    private FlowEnrollmentRepository enrollmentRepository;
    @Autowired
    private EmailSendRepository sendRepository;
    @Autowired
    private DiscountCodeRepository discountRepository;
    @Autowired
    private NewsletterSubscriberRepository subscriberRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CookieRepository cookieRepository;
    @Autowired
    private CookieVariantRepository variantRepository;

    private String single;

    @BeforeEach
    void setUp() {
        Cookie cookie = cookieRepository.save(new Cookie.Builder().setDescription("Lemon Almond").setSlug("lemon-almond-flow")
                .setCategory(CookieCategory.LOW_SUGAR).setPrice(30.0).setStockQuantity(24)
                .setIngredients("Almond flour").setAllergies("Tree nuts").build());
        single = variantRepository.save(new CookieVariant.Builder().setCookieId(cookie.getCookieId())
                .setLabel("Single").setUnitsPerPack(1).setPrice(new BigDecimal("30")).build()).getVariantId();
    }

    @Test
    void aNewSubscriberGetsOnePersonalCodeAndAPurchaseEndsTheSeries() {
        String email = "welcome@example.co.za";
        newsletterService.subscribe(email, "footer");
        newsletterService.subscribe(email, "footer");

        assertEquals(1, runner.runDue());

        FlowEnrollment welcome = only(email, EmailFlow.WELCOME);
        assertEquals(1, welcome.getCurrentStep());
        assertTrue(welcome.getNextSendAt().isAfter(LocalDateTime.now().plusHours(23)));
        DiscountCode code = discountRepository.findByCode(welcome.getDiscountCode()).orElseThrow();
        assertTrue(code.getCode().startsWith("WELCOME-"));
        assertEquals(email, code.getRestrictedToEmail());
        assertEquals(1, code.getMaxUses());

        enrollments.purchased(email);
        assertEquals(EnrollmentStatus.EXITED, only(email, EmailFlow.WELCOME).getStatus());
    }

    @Test
    void anIdleCartIsRemindedAndItsLinkReopensIt() {
        String email = "cart@example.co.za";
        subscriberRepository.save(new NewsletterSubscriber(email, Instant.now()));
        String cartId = idleCart(email);
        String strangersCart = idleCart("stranger@example.co.za");

        triggers.findAbandonedCarts();
        dueNow(email);
        assertEquals(1, runner.runDue());

        Cart cart = cartRepository.findById(cartId).orElseThrow();
        assertNotNull(cart.getRecoveryToken());
        assertEquals(cartId, linkService.recoverCart(cart.getRecoveryToken()).cartId());
        assertEquals(1, only(email, EmailFlow.CART_ABANDONMENT).getCurrentStep());
        assertTrue(enrollmentRepository.findAll().stream().noneMatch(e -> e.getContextRef() != null
                && e.getContextRef().equals(strangersCart)), "no consent, no reminder");
    }

    @Test
    void aClickIsTrackedAndOnlyLeadsToTheShop() {
        String email = "clicker@example.co.za";
        newsletterService.subscribe(email, "footer");
        runner.runDue();
        EmailSend send = sendRepository.findAll().stream().filter(s -> s.getEmail().equals(email)).findFirst().orElseThrow();

        assertEquals(links.frontend("/products"), trackingService.clicked(send.getSendId(), links.frontend("/products")));
        assertEquals(links.frontend("/"), trackingService.clicked(send.getSendId(), "https://evil.example.com"));
        assertNotNull(sendRepository.findById(send.getSendId()).orElseThrow().getClickedAt());
        assertNotNull(subscriberRepository.findById(email).orElseThrow().getLastEngagedAt());
    }

    @Test
    void unsubscribingStopsEveryFlowUntilTheyOptInAgain() {
        String email = "leaving@example.co.za";
        newsletterService.subscribe(email, "footer");

        linkService.unsubscribe(email, links.token(email), "Unsubscribe page");

        assertEquals(EnrollmentStatus.EXITED, only(email, EmailFlow.WELCOME).getStatus());
        for (EmailFlow flow : EmailFlow.values()) {
            assertFalse(consent.canSend(email, flow));
        }

        newsletterService.subscribe(email, "footer");
        assertTrue(consent.canSend(email, EmailFlow.WELCOME));
        assertEquals(1, enrollmentRepository.findAll().stream()
                .filter(e -> e.getEmail().equals(email) && e.getFlow() == EmailFlow.WELCOME).count(), "one welcome, ever");
    }

    private String idleCart(String email) {
        String cartId = cartService.create(null).cartId();
        cartService.addItem(cartId, new AddItemRequest(single, 2));
        cartService.setEmail(cartId, email);
        Cart cart = cartRepository.findById(cartId).orElseThrow();
        cartRepository.save(new Cart.Builder().copy(cart).setUpdatedAt(LocalDateTime.now().minusHours(2)).build());
        return cartId;
    }

    // Quiet hours could otherwise push the first reminder to tomorrow morning.
    private void dueNow(String email) {
        for (FlowEnrollment enrollment : enrollmentRepository.findByEmailAndStatus(email, EnrollmentStatus.ACTIVE)) {
            enrollmentRepository.save(new FlowEnrollment.Builder().copy(enrollment).setNextSendAt(LocalDateTime.now()).build());
        }
    }

    private FlowEnrollment only(String email, EmailFlow flow) {
        List<FlowEnrollment> found = enrollmentRepository.findAll().stream()
                .filter(e -> e.getEmail().equals(email) && e.getFlow() == flow).toList();
        assertEquals(1, found.size());
        return found.getFirst();
    }
}
