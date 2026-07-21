package za.ac.cput.guiltfreecookie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "za.ac.cput.guiltfreecookie.domain")
@EnableJpaRepositories(basePackages = "za.ac.cput.guiltfreecookie.repository")
public class GuiltFreeCookieApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuiltFreeCookieApplication.class, args);
    }

}
