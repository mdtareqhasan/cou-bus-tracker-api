package com.cou.bustracker.cli;

import com.cou.bustracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * One-off JWT minting, invoked from inside the running container:
 *
 * <pre>
 * jwt-cli --gen-jwt --email=... --role=STUDENT
 * </pre>
 *
 * <p>Mirrors {@link AccountCli}: {@code CouBusTrackerApplication} disables the
 * web server when it sees {@code --gen-jwt}, and the JVM exits once the token
 * is printed. The token is signed by the very same {@link JwtService} used at
 * login (identical secret handling and claims: {@code sub}/{@code role}/
 * {@code iat}/{@code exp}), so any instance sharing {@code JWT_SECRET}
 * accepts it. The email must still exist and be active in the matching table —
 * the API re-checks that on every request.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtCli implements ApplicationRunner {

    private static final Set<String> ROLES = Set.of("STUDENT", "TEACHER", "ADMIN", "SUPER_ADMIN");

    private final JwtService jwtService;
    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("gen-jwt")) {
            return;
        }

        int exitCode;
        try {
            String email = option(args, "email");
            String role = option(args, "role");
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("--email is required");
            }
            if (role == null || role.isBlank()) {
                throw new IllegalArgumentException(
                        "--role is required: STUDENT, TEACHER, ADMIN or SUPER_ADMIN");
            }
            role = role.trim().toUpperCase();
            if (!ROLES.contains(role)) {
                throw new IllegalArgumentException(
                        "Unknown role '" + role + "'; use one of " + ROLES);
            }

            String token = jwtService.generateToken(email.trim().toLowerCase(), role);
            // A JWT always starts with "eyJ" — extract it from the surrounding
            // startup/shutdown logs with: ... | grep '^eyJ'
            System.out.println(token);
            exitCode = 0;
        } catch (RuntimeException e) {
            log.error("JWT generation failed: {}", e.getMessage());
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
