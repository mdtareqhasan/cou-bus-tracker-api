package com.cou.bustracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CouBusTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CouBusTrackerApplication.class, args);
    }
}
