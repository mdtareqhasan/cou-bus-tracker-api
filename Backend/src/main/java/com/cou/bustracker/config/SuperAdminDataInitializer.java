package com.cou.bustracker.config;

import com.cou.bustracker.entity.SuperAdmin;
import com.cou.bustracker.repository.SuperAdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Ensures the default super admin exists after Flyway migration has run.
 * The hash for "Admin@123" is computed at startup using the same
 * BCryptPasswordEncoder bean used by the rest of the system, so it is
 * guaranteed to verify.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SuperAdminDataInitializer {

    private static final String DEFAULT_EMAIL = "superadmincou@gmail.com";
    private static final String DEFAULT_PASSWORD = "Admin@123";
    private static final String DEFAULT_FULL_NAME = "System Super Admin";

    @Bean
    public ApplicationRunner seedSuperAdmin(SuperAdminRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (repo.findByEmail(DEFAULT_EMAIL).isEmpty()) {
                SuperAdmin sa = SuperAdmin.builder()
                        .email(DEFAULT_EMAIL)
                        .password(encoder.encode(DEFAULT_PASSWORD))
                        .fullName(DEFAULT_FULL_NAME)
                        .isActive(Boolean.TRUE)
                        .build();
                repo.save(sa);
                log.info("Seeded default super admin: {}", DEFAULT_EMAIL);
            } else {
                log.info("Default super admin already present: {}", DEFAULT_EMAIL);
            }
        };
    }
}