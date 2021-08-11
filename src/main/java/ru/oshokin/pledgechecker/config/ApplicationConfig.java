package ru.oshokin.pledgechecker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.oshokin.pledgechecker.services.PledgeService;

@Configuration
@Slf4j
public class ApplicationConfig {

    @Bean
    public CommandLineRunner startWorkers(@Autowired PledgeService pledgeService) {
        return args -> pledgeService.startBrowsers();
    }

}