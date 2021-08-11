package ru.oshokin.pledgechecker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("ru.oshokin.pledgechecker")
public class PledgeCheckerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PledgeCheckerApplication.class, args);
    }

}
