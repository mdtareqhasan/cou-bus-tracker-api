package com.cou.bustracker.service;

import com.cou.bustracker.entity.Admin;
import com.cou.bustracker.entity.SuperAdmin;
import com.cou.bustracker.repository.AdminRepository;
import com.cou.bustracker.repository.SuperAdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Creates the first administrator accounts from the command line.
 *
 * <p>Kept separate from the HTTP admin-management endpoints so the same rules
 * (email normalisation, password policy, duplicate check) apply to both.
 */
@Service
@RequiredArgsConstructor
public class AccountProvisioningService {

    /** BCrypt only hashes the first 72 bytes; longer passwords silently differ. */
    static final int MAX_PASSWORD_LENGTH = 72;
    static final int MIN_PASSWORD_LENGTH = 8;

    private final AdminRepository adminRepository;
    private final SuperAdminRepository superAdminRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Admin createAdmin(String email, String password, String name) {
        String normalizedEmail = normalizeEmail(email);
        validatePassword(password);
        if (adminRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException(
                    "An admin with email " + normalizedEmail + " already exists");
        }

        Admin admin = Admin.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(password))
                .name(blankToDefault(name, "Administrator"))
                .build();
        return adminRepository.save(admin);
    }

    @Transactional
    public SuperAdmin createSuperAdmin(String email, String password, String fullName) {
        String normalizedEmail = normalizeEmail(email);
        validatePassword(password);
        if (superAdminRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException(
                    "A super admin with email " + normalizedEmail + " already exists");
        }

        SuperAdmin superAdmin = SuperAdmin.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(password))
                .fullName(blankToDefault(fullName, "Super Administrator"))
                .isActive(Boolean.TRUE)
                .build();
        return superAdminRepository.save(superAdmin);
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("--email is required");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int at = normalized.indexOf('@');
        if (at <= 0 || at != normalized.lastIndexOf('@') || at == normalized.length() - 1) {
            throw new IllegalArgumentException("Enter a valid email address");
        }
        return normalized;
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("--password is required");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at most " + MAX_PASSWORD_LENGTH + " characters");
        }
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
