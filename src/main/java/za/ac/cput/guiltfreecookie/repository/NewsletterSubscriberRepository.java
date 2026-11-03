package za.ac.cput.guiltfreecookie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.guiltfreecookie.domain.NewsletterSubscriber;

import java.time.Instant;
import java.util.List;

@Repository
public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, String> {

    // Active subscribers who joined before the cutoff and haven't opened or clicked since it.
    @Query("select s from NewsletterSubscriber s where (s.status is null or s.status = za.ac.cput.guiltfreecookie.domain.SubscriberStatus.ACTIVE)"
            + " and s.subscribedAt < :cutoff and (s.lastEngagedAt is null or s.lastEngagedAt < :cutoff)")
    List<NewsletterSubscriber> findDisengaged(@Param("cutoff") Instant cutoff);
}
