package com.cou.bustracker.service;

import com.cou.bustracker.entity.Admin;
import com.cou.bustracker.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminManagementServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminManagementService adminManagementService;

    @Test
    void deleteAdmin_shouldRejectDeletingYourOwnAccount() {
        Admin currentAdmin = Admin.builder()
                .id(1L)
                .email("me@example.com")
                .name("Me")
                .password("encoded")
                .build();

        when(adminRepository.findByEmail("me@example.com")).thenReturn(Optional.of(currentAdmin));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> adminManagementService.deleteAdmin("me@example.com", 1L));

        assertEquals("You cannot delete your own account", ex.getMessage());
    }
}
