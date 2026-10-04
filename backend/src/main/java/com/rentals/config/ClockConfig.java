package com.rentals.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    @Value("${rentals.time-zone:America/Guayaquil}")
    private String timeZone;

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(timeZone));
    }
}
