package za.ac.cput.guiltfreecookie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class GuiltFreeCookieApplication {

    // Times are stored and shown as shop time, so the app runs in Cape Town time whatever the host's zone.
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Johannesburg"));
        SpringApplication.run(GuiltFreeCookieApplication.class, args);
    }

}
