package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.CreateAdminRequest;
import com.cou.bustracker.dto.request.UpdateAdminProfileRequest;
import com.cou.bustracker.dto.response.AdminProfileResponse;
import com.cou.bustracker.entity.Admin;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public List<AdminProfileResponse> getAllAdmins() {
        return adminRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminProfileResponse createAdmin(CreateAdminRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (adminRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An admin with this email already exists");
        }

        Admin admin = Admin.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        return mapToResponse(adminRepository.save(admin));
    }

    @Transactional
    public AdminProfileResponse updateAdmin(Long adminId, UpdateAdminProfileRequest request) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        admin.setName(request.getName().trim());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            admin.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return mapToResponse(adminRepository.save(admin));
    }

    @Transactional
    public void deleteAdmin(String currentAdminEmail, Long adminId) {
        Admin currentAdmin = adminRepository.findByEmail(currentAdminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (currentAdmin.getId().equals(adminId)) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }

        Admin adminToDelete = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        adminRepository.delete(adminToDelete);
    }

    private AdminProfileResponse mapToResponse(Admin admin) {
        return AdminProfileResponse.builder()
                .id(admin.getId())
                .name(admin.getName())
                .email(admin.getEmail())
                .build();
    }
}
