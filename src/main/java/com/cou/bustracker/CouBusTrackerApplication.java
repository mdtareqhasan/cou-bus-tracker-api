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
        if (isAccountCommand(args)) {
            // A one-off account command must not bind port 8080 (the running API
            // already owns it) or start any background work.
            application.setWebApplicationType(WebApplicationType.NONE);
            application.setAdditionalProfiles("cli");
        }
        application.run(args);
    }

    private static boolean isAccountCommand(String[] args) {
        return Arrays.stream(args).anyMatch(arg ->
                arg.equals("--create-admin") || arg.startsWith("--create-admin=")
                        || arg.equals("--create-super-admin")
                        || arg.startsWith("--create-super-admin="));
    }
}
