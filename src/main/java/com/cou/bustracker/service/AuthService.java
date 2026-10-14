package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.LoginRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.entity.Admin;
import com.cou.bustracker.repository.AdminRepository;
import com.cou.bustracker.security.CustomUserDetailsService;
import com.cou.bustracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminRepository adminRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        String token = jwtService.generateToken(admin.getEmail(), "ADMIN");

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role("ADMIN")
                .id(admin.getId())
                .name(admin.getName())
                .email(admin.getEmail())
                .build();
    }

    public Admin register(String email, String password, String name) {
        if (adminRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        Admin admin = Admin.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .build();

        return adminRepository.save(admin);
    }
}
