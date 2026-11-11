package com.hyojinahn.shiptime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ShipTimeRateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShipTimeRateApplication.class, args);
    }
}
