package za.ac.cput.guiltfreecookie.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import za.ac.cput.guiltfreecookie.domain.DiscountCode;
import za.ac.cput.guiltfreecookie.domain.DiscountType;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class DiscountCodeRepositoryTest {

    @Autowired
    private DiscountCodeRepository repository;

    @Test
    void redeemStopsAtMaxUses() {
        repository.save(new DiscountCode.Builder().setCode("ONCE").setType(DiscountType.FREE_SHIPPING).setMaxUses(1).build());
        repository.save(new DiscountCode.Builder().setCode("ALWAYS").setType(DiscountType.FREE_SHIPPING).build());

        assertEquals(1, repository.redeem("ONCE"));
        assertEquals(0, repository.redeem("ONCE"));
        assertEquals(1, repository.redeem("ALWAYS"));
        assertEquals(1, repository.redeem("ALWAYS"));
        assertEquals(2, repository.findByCode("ALWAYS").orElseThrow().getUsedCount());
    }
}
