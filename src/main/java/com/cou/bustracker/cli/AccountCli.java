package com.cou.bustracker.cli;

import com.cou.bustracker.entity.Admin;
import com.cou.bustracker.entity.SuperAdmin;
import com.cou.bustracker.service.AccountProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * One-off account creation, invoked from inside the running container:
 *
 * <pre>
 * account-cli --create-admin --email=... --password=... --name="..."
 * account-cli --create-super-admin --email=... --password=... --name="..."
 * </pre>
 *
 * <p>{@code CouBusTrackerApplication} switches off the web server when it sees
 * either flag, so this JVM never tries to bind port 8080. The process exits as
 * soon as the account is created (exit code 0) or the attempt fails (1).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountCli implements ApplicationRunner {

    private final AccountProvisioningService provisioningService;
    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        boolean createAdmin = args.containsOption("create-admin");
        boolean createSuperAdmin = args.containsOption("create-super-admin");
        if (!createAdmin && !createSuperAdmin) {
            return;
        }

        int exitCode;
        try {
            if (createAdmin && createSuperAdmin) {
                throw new IllegalArgumentException(
                        "Use either --create-admin or --create-super-admin, not both");
            }

            String email = option(args, "email");
            String password = option(args, "password");
            String name = option(args, "name");

            if (createAdmin) {
                Admin admin = provisioningService.createAdmin(email, password, name);
                log.info("Admin created: {} (id={})", admin.getEmail(), admin.getId());
            } else {
                SuperAdmin superAdmin =
                        provisioningService.createSuperAdmin(email, password, name);
                log.info("Super admin created: {} (id={})",
                        superAdmin.getEmail(), superAdmin.getId());
            }
            exitCode = 0;
        } catch (RuntimeException e) {
            log.error("Account creation failed: {}", e.getMessage());
            exitCode = 1;
        }

        int finalExitCode = exitCode;
        System.exit(SpringApplication.exit(applicationContext, () -> finalExitCode));
    }

    private static String option(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
