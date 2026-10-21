package com.cou.bustracker.service;

import com.cou.bustracker.entity.Admin;
import com.cou.bustracker.entity.SuperAdmin;
import com.cou.bustracker.repository.AdminRepository;
import com.cou.bustracker.repository.SuperAdminRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountProvisioningServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private SuperAdminRepository superAdminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountProvisioningService service;

    @Test
    void createAdmin_normalisesEmailEncodesPasswordAndDefaultsName() {
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Admin admin = service.createAdmin(" Admin@Example.COM ", "secret123", null);

        assertEquals("admin@example.com", admin.getEmail());
        assertEquals("bcrypt-hash", admin.getPassword());
        assertEquals("Administrator", admin.getName());
        verify(adminRepository).save(admin);
    }

    @Test
    void createAdmin_usesProvidedName() {
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Admin admin = service.createAdmin("admin@example.com", "secret123", "  COU Admin  ");

        assertEquals("COU Admin", admin.getName());
    }

    @Test
    void createSuperAdmin_normalisesEmailAndCreatesActiveAccount() {
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");
        when(superAdminRepository.save(any(SuperAdmin.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SuperAdmin superAdmin =
                service.createSuperAdmin("Super@Example.COM", "secret123", null);

        assertEquals("super@example.com", superAdmin.getEmail());
        assertEquals("bcrypt-hash", superAdmin.getPassword());
        assertEquals("Super Administrator", superAdmin.getFullName());
        assertTrue(superAdmin.getIsActive());
    }

    @Test
    void createAdmin_rejectsDuplicateEmailWithoutSaving() {
        when(adminRepository.existsByEmail("admin@example.com")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.createAdmin("admin@example.com", "secret123", "Admin"));

        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void createSuperAdmin_rejectsDuplicateEmailWithoutSaving() {
        when(superAdminRepository.existsByEmail("super@example.com")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.createSuperAdmin("super@example.com", "secret123", "Super"));

        verify(superAdminRepository, never()).save(any(SuperAdmin.class));
    }

    @Test
    void rejectsShortPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createAdmin("admin@example.com", "short", "Admin"));
    }

    @Test
    void rejectsOverlongPassword() {
        String password = "a".repeat(AccountProvisioningService.MAX_PASSWORD_LENGTH + 1);

        assertThrows(IllegalArgumentException.class,
                () -> service.createAdmin("admin@example.com", password, "Admin"));
    }

    @Test
    void rejectsInvalidEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createAdmin("not-an-email", "secret123", "Admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createSuperAdmin("two@@example.com", "secret123", "Super"));
    }

    @Test
    void rejectsMissingEmailAndPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createAdmin(null, "secret123", "Admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createAdmin("admin@example.com", " ", "Admin"));
    }
}
