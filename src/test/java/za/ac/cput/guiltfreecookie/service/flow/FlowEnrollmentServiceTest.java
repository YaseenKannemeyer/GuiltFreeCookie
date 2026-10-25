package za.ac.cput.guiltfreecookie.service.flow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import za.ac.cput.guiltfreecookie.domain.Cart;
import za.ac.cput.guiltfreecookie.domain.CartStatus;
import za.ac.cput.guiltfreecookie.domain.EmailFlow;
import za.ac.cput.guiltfreecookie.domain.EmailFlowSettings;
import za.ac.cput.guiltfreecookie.domain.EnrollmentStatus;
import za.ac.cput.guiltfreecookie.domain.FlowEnrollment;
import za.ac.cput.guiltfreecookie.repository.CartRepository;
import za.ac.cput.guiltfreecookie.repository.FlowEnrollmentRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FlowEnrollmentServiceTest {

    private static final String EMAIL = "thandi@example.co.za";

    @Mock
    private FlowEnrollmentRepository repository;
    @Mock
    private EmailFlowSettingsService settingsService;
    @Mock
    private MarketingConsent consent;
    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private FlowEnrollmentService service;

    @BeforeEach
    void allFlowsOnAndAllowed() {
        when(settingsService.get()).thenReturn(settings(true));
        when(consent.canSend(any(), any())).thenReturn(true);
    }

    @Test
    void theWelcomeGoesOutOnceAndStraightAway() {
        service.welcome(EMAIL);

        verify(repository).save(argThat(e -> e.getFlow() == EmailFlow.WELCOME && e.getStatus() == EnrollmentStatus.ACTIVE
                && !e.getNextSendAt().isAfter(LocalDateTime.now())));

        when(repository.existsByEmailAndFlow(EMAIL, EmailFlow.WELCOME)).thenReturn(true);
        service.welcome(EMAIL);
        verify(repository).save(any());
    }

    @Test
    void nobodyIsEnrolledWithoutConsentOrWhenTheFlowIsOff() {
        when(consent.canSend(EMAIL, EmailFlow.WELCOME)).thenReturn(false);
        service.welcome(EMAIL);

        when(consent.canSend(EMAIL, EmailFlow.WELCOME)).thenReturn(true);
        when(settingsService.get()).thenReturn(settings(false));
        service.welcome(EMAIL);

        verify(repository, never()).save(any());
    }

    @Test
    void browseRemindersAreAtMostWeekly() {
        when(repository.existsByEmailAndFlowAndEnrolledAtAfter(eq(EMAIL), eq(EmailFlow.BROWSE_ABANDONMENT), any())).thenReturn(true);

        service.browse(EMAIL, "c1");

        verify(repository, never()).save(any());
    }

    @Test
    void anAbandonedCartGetsARecoveryTokenAndIsOnlyEnrolledOnce() {
        Cart cart = new Cart.Builder().setCartId("cart-1").setCustomerEmail(EMAIL).setStatus(CartStatus.ACTIVE).build();

        service.abandonedCart(cart);

        verify(cartRepository).save(argThat(saved -> saved.getRecoveryToken() != null && saved.getRecoveryToken().length() >= 32));
        verify(repository).save(argThat(e -> e.getFlow() == EmailFlow.CART_ABANDONMENT && "cart-1".equals(e.getContextRef())));

        when(repository.existsByFlowAndContextRef(EmailFlow.CART_ABANDONMENT, "cart-1")).thenReturn(true);
        service.abandonedCart(cart);
        verify(repository).save(any());
    }

    @Test
    void aPurchaseEndsTheSellingFlows() {
        FlowEnrollment welcome = new FlowEnrollment.Builder().setEnrollmentId("e1").setEmail(EMAIL)
                .setFlow(EmailFlow.WELCOME).setStatus(EnrollmentStatus.ACTIVE).build();
        when(repository.findByEmailAndFlowInAndStatus(eq(EMAIL), any(), eq(EnrollmentStatus.ACTIVE))).thenReturn(List.of(welcome));

        service.purchased(EMAIL);

        verify(repository).findByEmailAndFlowInAndStatus(eq(EMAIL),
                argThat(flows -> flows.contains(EmailFlow.CART_ABANDONMENT) && !flows.contains(EmailFlow.POST_PURCHASE)),
                eq(EnrollmentStatus.ACTIVE));
        verify(repository).save(argThat(e -> e.getStatus() == EnrollmentStatus.EXITED && "Purchased".equals(e.getExitReason())));
    }

    @Test
    void scheduledEmailsWaitForShopHours() {
        ZoneId shop = FlowEnrollmentService.SHOP_ZONE;
        LocalDateTime lateNight = ZonedDateTime.of(2026, 10, 24, 23, 30, 0, 0, shop)
                .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime earlyMorning = ZonedDateTime.of(2026, 10, 24, 5, 0, 0, 0, shop)
                .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime midday = ZonedDateTime.of(2026, 10, 24, 12, 0, 0, 0, shop)
                .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

        assertEquals(ZonedDateTime.of(2026, 10, 25, 8, 0, 0, 0, shop), inShop(FlowEnrollmentService.sendTime(lateNight, false)));
        assertEquals(ZonedDateTime.of(2026, 10, 24, 8, 0, 0, 0, shop), inShop(FlowEnrollmentService.sendTime(earlyMorning, false)));
        assertEquals(midday, FlowEnrollmentService.sendTime(midday, false));
        assertEquals(lateNight, FlowEnrollmentService.sendTime(lateNight, true));
        assertEquals(LocalTime.of(8, 0), FlowEnrollmentService.SEND_FROM);
    }

    private static ZonedDateTime inShop(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).withZoneSameInstant(FlowEnrollmentService.SHOP_ZONE);
    }

    static EmailFlowSettings settings(boolean enabled) {
        return new EmailFlowSettings.Builder().setSettingsId("default")
                .setWelcomeEnabled(enabled).setBrowseEnabled(enabled).setCartEnabled(enabled).setPostPurchaseEnabled(enabled)
                .setReplenishmentEnabled(enabled).setWinbackEnabled(enabled).setSunsetEnabled(enabled)
                .setWelcomeDiscountPercent(10).setWelcomeCodeDays(5).setCartDiscountPercent(10).setWinbackDiscountPercent(15)
                .setReplenishmentDays(21).setWinbackDays(60).setSunsetDays(90).build();
    }
}
