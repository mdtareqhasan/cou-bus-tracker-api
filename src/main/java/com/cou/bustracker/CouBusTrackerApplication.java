package com.cou.bustracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

@SpringBootApplication
@EnableScheduling
public class CouBusTrackerApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(CouBusTrackerApplication.class);
        if (isCliCommand(args)) {
            // A one-off CLI command (account creation, JWT minting) must not bind
            // port 8080 (the running API already owns it) or start background work.
            application.setWebApplicationType(WebApplicationType.NONE);
            application.setAdditionalProfiles("cli");
        }
        application.run(args);
    }

    private static boolean isCliCommand(String[] args) {
        return Arrays.stream(args).anyMatch(arg ->
                arg.equals("--create-admin") || arg.startsWith("--create-admin=")
                        || arg.equals("--create-super-admin")
                        || arg.startsWith("--create-super-admin=")
                        || arg.equals("--gen-jwt") || arg.startsWith("--gen-jwt="));
    }
}
