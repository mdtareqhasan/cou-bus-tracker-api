package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.TeacherRegisterRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.dto.response.TeacherResponse;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.TeacherRepository;
import com.cou.bustracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    private static final String EDU_MAIL_DOMAIN = "cou.ac.bd";

    @Transactional
    public AuthResponse register(TeacherRegisterRequest request) {
        if (teacherRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        boolean isEduMail = request.getEmail().endsWith("@" + EDU_MAIL_DOMAIN);

        Teacher teacher = Teacher.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .designation(request.getDesignation())
                .department(request.getDepartment())
                .phone(request.getPhone())
                .isEduMail(isEduMail)
                .isVerified(false)
                .isActive(true)
                .build();

        teacherRepository.save(teacher);

        String token = jwtService.generateToken(teacher.getEmail());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .adminName(teacher.getName())
                .build();
    }

    public AuthResponse login(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        Teacher teacher = teacherRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        String token = jwtService.generateToken(teacher.getEmail());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .adminName(teacher.getName())
                .build();
    }

    public List<TeacherResponse> getAllTeachers() {
        return teacherRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TeacherResponse> getPendingTeachers() {
        return teacherRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(t -> !t.getIsVerified())
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
        teacherRepository.delete(teacher);
    }

    private TeacherResponse mapToResponse(Teacher teacher) {
        return TeacherResponse.builder()
                .id(teacher.getId())
                .name(teacher.getName())
                .email(teacher.getEmail())
                .designation(teacher.getDesignation())
                .department(teacher.getDepartment())
                .phone(teacher.getPhone())
                .isEduMail(teacher.getIsEduMail())
                .isVerified(teacher.getIsVerified())
                .isActive(teacher.getIsActive())
                .createdAt(teacher.getCreatedAt())
                .build();
    }
}
