package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.UpdateAdminProfileRequest;
import com.cou.bustracker.dto.response.AdminProfileResponse;
import com.cou.bustracker.entity.Admin;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminProfileService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminProfileResponse getProfile(String email) {
        return mapToResponse(findAdmin(email));
    }

    @Transactional
    public AdminProfileResponse updateProfile(String email, UpdateAdminProfileRequest request) {
        Admin admin = findAdmin(email);
        admin.setName(request.getName().trim());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            admin.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return mapToResponse(adminRepository.save(admin));
    }

    private Admin findAdmin(String email) {
        return adminRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
    }

    private AdminProfileResponse mapToResponse(Admin admin) {
        return AdminProfileResponse.builder()
                .id(admin.getId())
                .name(admin.getName())
                .email(admin.getEmail())
                .build();
    }
}
