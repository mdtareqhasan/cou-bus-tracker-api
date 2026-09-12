package com.cou.bustracker.service;

import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.dto.response.TeacherResponse;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.TeacherRepository;
import com.cou.bustracker.security.JwtService;
import com.cou.bustracker.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Teacher read/write operations used after the user has been verified by
 * {@link PhoneVerificationService}. Registration itself lives in the OTP
 * service — see {@code POST /api/auth/phone-verification/init}.
 */
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public AuthResponse loginWithPhone(String phone, String password) {
        String normalized = PhoneUtils.normalizeBd(phone);
        Teacher teacher = teacherRepository.findByPhone(normalized)
                .orElseThrow(() -> new RuntimeException("Teacher not found with this phone number"));
        if (teacher.getPassword() == null || !passwordEncoder.matches(password, teacher.getPassword())) {
            throw new BadCredentialsException("Invalid phone number or password");
        }
        if (!teacher.getIsActive()) {
            throw new BadCredentialsException("Account is deactivated. Please contact admin.");
        }
        if (!teacher.getIsPhoneVerified()) {
            throw new BadCredentialsException("Please verify your phone number before logging in");
        }

        String token = jwtService.generateToken(teacher.getPhone(), "TEACHER");

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role("TEACHER")
                .id(teacher.getId())
                .name(teacher.getName())
                .phone(teacher.getPhone())
                .isVerified(teacher.getIsVerified())
                .isPhoneVerified(teacher.getIsPhoneVerified())
                .build();
    }

    @Transactional
    public void uploadIdCard(Long teacherId, String imageUrl) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        teacher.setIdCardImageUrl(imageUrl);
        teacherRepository.save(teacher);
    }

    public List<TeacherResponse> getAllTeachers() {
        return teacherRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(teacher -> Boolean.TRUE.equals(teacher.getIsPhoneVerified()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Teacher getTeacherByPhone(String phone) {
        return teacherRepository.findByPhone(PhoneUtils.normalizeBd(phone))
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
    }

    public TeacherResponse getProfile(String phone) {
        return mapToResponse(getTeacherByPhone(phone));
    }

    public List<TeacherResponse> getPendingTeachers() {
        return teacherRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(teacher -> Boolean.TRUE.equals(teacher.getIsPhoneVerified()))
                .filter(teacher -> !Boolean.TRUE.equals(teacher.getIsVerified()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void verifyTeacher(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        teacher.setIsVerified(true);
        teacherRepository.save(teacher);
    }

    @Transactional
    public void deactivateTeacher(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        teacher.setIsActive(!teacher.getIsActive());
        teacherRepository.save(teacher);
    }

    @Transactional
    public void deleteTeacher(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        if (teacher.getIdCardImageUrl() != null) {
            deleteIdCardFile(teacher.getIdCardImageUrl());
        }
        teacherRepository.delete(teacher);
    }

    private void deleteIdCardFile(String imageUrl) {
        fileStorageService.deleteFile(imageUrl);
    }

    private TeacherResponse mapToResponse(Teacher teacher) {
        return TeacherResponse.builder()
                .id(teacher.getId())
                .name(teacher.getName())
                .teacherId(teacher.getTeacherId())
                .designation(teacher.getDesignation())
                .department(teacher.getDepartment())
                .phone(teacher.getPhone())
                .idCardImageUrl(teacher.getIdCardImageUrl())
                .isVerified(teacher.getIsVerified())
                .isPhoneVerified(teacher.getIsPhoneVerified())
                .isActive(teacher.getIsActive())
                .createdAt(teacher.getCreatedAt())
                .build();
    }
}
