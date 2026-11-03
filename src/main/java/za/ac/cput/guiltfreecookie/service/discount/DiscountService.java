package za.ac.cput.guiltfreecookie.service.discount;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.guiltfreecookie.domain.DiscountCode;
import za.ac.cput.guiltfreecookie.domain.DiscountType;
import za.ac.cput.guiltfreecookie.dto.discount.DiscountRequest;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.ConflictException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.DiscountCodeRepository;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class DiscountService {

    static final String EMAIL_NEEDED = "It applies once you enter your email at checkout";

    static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{2,29}$");
    static final BigDecimal MAX_FIXED = BigDecimal.valueOf(100_000);
    static final int MAX_DESCRIPTION_LENGTH = 150;
    static final String NOT_VALID = "This code isn't valid";

    public record Evaluation(DiscountCode discount, boolean applied, BigDecimal amount, boolean freeShipping, String message) {
    }

    private final DiscountCodeRepository repository;

    public DiscountService(DiscountCodeRepository repository) {
        this.repository = repository;
    }

    // For the cart view: never throws, explains why a code isn't counting.
    public Evaluation evaluate(String code, BigDecimal subtotal, String email) {
        DiscountCode discount = code == null ? null : repository.findByCode(code).orElse(null);
        String problem = problem(discount, subtotal, email, LocalDateTime.now());
        if (problem != null) {
            return new Evaluation(discount, false, BigDecimal.ZERO.setScale(2), false, problem);
        }
        return new Evaluation(discount, true, amount(discount, subtotal), discount.getType() == DiscountType.FREE_SHIPPING, null);
    }

    // For "apply code": rejects a code that wouldn't count right now. A personal code is accepted before
    // the cart has an email (the cart page doesn't ask for one); checkout checks it against the email.
    public DiscountCode requireApplicable(String rawCode, BigDecimal subtotal, String email) {
        String code = normalize(rawCode);
        DiscountCode discount = code == null ? null : repository.findByCode(code).orElse(null);
        String problem = problem(discount, subtotal, email, LocalDateTime.now());
        if (problem != null && !problem.equals(EMAIL_NEEDED)) {
            throw new BadRequestException(problem);
        }
        return discount;
    }

    // Called when an order is placed. False if another order took the last use first.
    @Transactional
    public boolean redeem(String code) {
        return repository.redeem(code) == 1;
    }

    @Transactional
    public void release(String code) {
        repository.release(code);
    }

    public List<DiscountCode> list() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public DiscountCode get(String discountId) {
        return repository.findById(discountId).orElseThrow(() -> new NotFoundException("Discount code not found"));
    }

    public DiscountCode create(DiscountRequest request) {
        DiscountCode validated = validated(request, new DiscountCode.Builder().setCreatedAt(LocalDateTime.now()));
        if (repository.existsByCode(validated.getCode())) {
            throw new ConflictException("A discount with code " + validated.getCode() + " already exists");
        }
        return repository.save(validated);
    }

    // usedCount is kept; everything else is replaced.
    public DiscountCode update(String discountId, DiscountRequest request) {
        DiscountCode existing = get(discountId);
        DiscountCode validated = validated(request, new DiscountCode.Builder().copy(existing));
        if (!validated.getCode().equals(existing.getCode()) && repository.existsByCode(validated.getCode())) {
            throw new ConflictException("A discount with code " + validated.getCode() + " already exists");
        }
        return repository.save(validated);
    }

    public DiscountCode deactivate(String discountId) {
        return repository.save(new DiscountCode.Builder().copy(get(discountId)).setActive(false).build());
    }

    public static String normalize(String code) {
        String cleaned = Helper.clean(code);
        return Helper.isNullOrEmpty(cleaned) ? null : cleaned.toUpperCase(Locale.ROOT);
    }

    static String problem(DiscountCode discount, BigDecimal subtotal, String email, LocalDateTime now) {
        if (discount == null || !discount.isActive()) {
            return NOT_VALID;
        }
        if (discount.getStartsAt() != null && now.isBefore(discount.getStartsAt())) {
            return "This code isn't active yet";
        }
        if (discount.getEndsAt() != null && !now.isBefore(discount.getEndsAt())) {
            return "This code has expired";
        }
        if (discount.isUsedUp()) {
            return "This code has already been used";
        }
        if (discount.getRestrictedToEmail() != null) {
            if (email == null) {
                return EMAIL_NEEDED;
            }
            if (!discount.getRestrictedToEmail().equalsIgnoreCase(email)) {
                return "This code belongs to a different email address";
            }
        }
        if (discount.getMinSubtotal() != null && subtotal.compareTo(discount.getMinSubtotal()) < 0) {
            return "Spend R" + discount.getMinSubtotal().setScale(2, RoundingMode.HALF_UP) + " or more to use this code";
        }
        return null;
    }

    static BigDecimal amount(DiscountCode discount, BigDecimal subtotal) {
        return switch (discount.getType()) {
            case PERCENT -> subtotal.multiply(discount.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED -> discount.getValue().min(subtotal).setScale(2, RoundingMode.HALF_UP);
            case FREE_SHIPPING -> BigDecimal.ZERO.setScale(2);
        };
    }

    private static DiscountCode validated(DiscountRequest request, DiscountCode.Builder builder) {
        String code = normalize(request.code());
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new BadRequestException("Code must be 3–30 letters, numbers, dashes or underscores");
        }
        if (request.type() == null) {
            throw new BadRequestException("Choose a discount type");
        }
        BigDecimal value = request.value();
        switch (request.type()) {
            case PERCENT -> {
                if (value == null || value.compareTo(BigDecimal.ONE) < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new BadRequestException("A percentage discount must be between 1 and 100");
                }
            }
            case FIXED -> {
                if (value == null || value.signum() <= 0 || value.compareTo(MAX_FIXED) > 0 || value.stripTrailingZeros().scale() > 2) {
                    throw new BadRequestException("A fixed discount must be a rand amount above 0");
                }
            }
            case FREE_SHIPPING -> value = null;
        }
        if (request.minSubtotal() != null && request.minSubtotal().signum() < 0) {
            throw new BadRequestException("Minimum order can't be negative");
        }
        if (request.startsAt() != null && request.endsAt() != null && !request.endsAt().isAfter(request.startsAt())) {
            throw new BadRequestException("The end date must be after the start date");
        }
        if (request.maxUses() != null && request.maxUses() < 1) {
            throw new BadRequestException("Max uses must be at least 1, or empty for unlimited");
        }
        String restrictedTo = Helper.cleanEmail(request.restrictedToEmail());
        if (Helper.isNullOrEmpty(restrictedTo)) {
            restrictedTo = null;
        } else if (!Helper.isValidEmail(restrictedTo)) {
            throw new BadRequestException("Enter a valid email address to restrict the code to");
        }
        String description = Helper.clean(request.description());
        if (!Helper.isWithin(description, MAX_DESCRIPTION_LENGTH)) {
            throw new BadRequestException("Description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        return builder
                .setCode(code)
                .setType(request.type())
                .setValue(value)
                .setMinSubtotal(request.minSubtotal())
                .setStartsAt(request.startsAt())
                .setEndsAt(request.endsAt())
                .setMaxUses(request.maxUses())
                .setOnePerEmail(Boolean.TRUE.equals(request.onePerEmail()))
                .setRestrictedToEmail(restrictedTo)
                .setDescription(description)
                .setActive(request.active() == null || request.active())
                .build();
    }
}
