package vn.ptit.tour.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {
    /** One injectable clock keeps price/window decisions reproducible in integration tests. */
    @Bean
    public Clock applicationClock() {
        return Clock.systemUTC();
    }
}
