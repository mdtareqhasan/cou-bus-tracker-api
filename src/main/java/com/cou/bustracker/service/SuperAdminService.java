package com.cou.bustracker.service;

import com.cou.bustracker.dto.superadmin.*;
import com.cou.bustracker.entity.SuperAdmin;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.exception.UnauthorizedException;
import com.cou.bustracker.repository.SuperAdminRepository;
import com.cou.bustracker.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final SuperAdminRepository superAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public SuperAdminLoginResponse login(SuperAdminLoginRequest request) {
        SuperAdmin superAdmin = superAdminRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!Boolean.TRUE.equals(superAdmin.getIsActive())) {
            throw new UnauthorizedException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), superAdmin.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtService.generateToken(superAdmin.getEmail(), "SUPER_ADMIN");

        // Extract issued/expired timestamps from the generated token.
        Date issued = jwtService.extractClaim(token, Claims::getIssuedAt);
        Date expires = jwtService.extractClaim(token, Claims::getExpiration);

        return SuperAdminLoginResponse.builder()
                .token(token)
                .issuedAt(toLocalDateTime(issued))
                .expiresAt(toLocalDateTime(expires))
                .superAdmin(mapToResponse(superAdmin))
                .build();
    }

    public List<SuperAdminResponse> getAllSuperAdmins() {
        return superAdminRepository.findAll().stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SuperAdminResponse getSuperAdminById(Long id) {
        return superAdminRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Super admin not found"));
    }

    @Transactional
    public SuperAdminResponse createSuperAdmin(SuperAdminCreateRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (superAdminRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("A super admin with this email already exists");
        }

        SuperAdmin superAdmin = SuperAdmin.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
                .build();

        return mapToResponse(superAdminRepository.save(superAdmin));
    }

    @Transactional
    public SuperAdminResponse updateSuperAdmin(Long id, SuperAdminUpdateRequest request) {
        SuperAdmin superAdmin = superAdminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Super admin not found"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            superAdmin.setFullName(request.getFullName().trim());
        }
        if (request.getIsActive() != null) {
            superAdmin.setIsActive(request.getIsActive());
        }
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            superAdmin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        return mapToResponse(superAdminRepository.save(superAdmin));
    }

    @Transactional
    public void deleteSuperAdmin(String currentSuperAdminEmail, Long id) {
        SuperAdmin current = superAdminRepository.findByEmail(currentSuperAdminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Super admin not found"));

        if (current.getId().equals(id)) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }

        SuperAdmin toDelete = superAdminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Super admin not found"));

        superAdminRepository.delete(toDelete);
    }

    public SuperAdmin findActiveByEmail(String email) {
        return superAdminRepository.findByEmail(email).orElse(null);
    }

    private SuperAdminResponse mapToResponse(SuperAdmin superAdmin) {
        return SuperAdminResponse.builder()
                .id(superAdmin.getId())
                .email(superAdmin.getEmail())
                .fullName(superAdmin.getFullName())
                .isActive(superAdmin.getIsActive())
                .createdAt(superAdmin.getCreatedAt())
                .updatedAt(superAdmin.getUpdatedAt())
                .build();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
    }
}