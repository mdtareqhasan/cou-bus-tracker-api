package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.StudentRegisterRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.dto.response.StudentResponse;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @Transactional
    public AuthResponse register(StudentRegisterRequest request, MultipartFile idCard) throws java.io.IOException {
        String phone = normalizePhone(request.getPhone());
        request.setPhone(phone);

        if (studentRepository.existsByPhone(phone)) {
            throw new RuntimeException("এই ফোন নম্বর ইতিমধ্যে ব্যবহৃত হয়েছে।");
        }
        if (studentRepository.existsByStudentId(request.getStudentId())) {
            throw new RuntimeException("এই শিক্ষার্থী ID ইতিমধ্যে নিবন্ধিত।");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        Student student = Student.builder()
                .name(request.getName())
                .phone(phone)
                .password(passwordEncoder.encode(request.getPassword()))
                .studentId(request.getStudentId())
                .department(request.getDepartment())
                .varsityBatch(request.getVarsityBatch())
                .isVerified(false)
                .isPhoneVerified(false)
                .isActive(true)
                .build();

        String imageUrl = fileStorageService.storeIdCard(idCard, "student-id-cards");
        student.setIdCardImageUrl(imageUrl);

        studentRepository.save(student);

        return AuthResponse.builder()
                .accessToken(null)
                .tokenType(null)
                .role("STUDENT")
                .id(student.getId())
                .name(student.getName())
                .phone(student.getPhone())
                .isVerified(student.getIsVerified())
                .isPhoneVerified(student.getIsPhoneVerified())
                .build();
    }

    public AuthResponse loginWithPhone(String phone, String password) {
        String normalized = normalizePhone(phone);
        Student student = studentRepository.findByPhone(normalized)
                .orElseThrow(() -> new RuntimeException("Student not found with this phone number"));
        if (student.getPassword() == null || !passwordEncoder.matches(password, student.getPassword())) {
            throw new BadCredentialsException("Invalid phone number or password");
        }
        if (!student.getIsActive()) {
            throw new BadCredentialsException("Account is deactivated. Please contact admin.");
        }
        if (!student.getIsPhoneVerified()) {
            throw new BadCredentialsException("Please verify your phone number before logging in");
        }

        String token = jwtService.generateToken(student.getPhone(), "STUDENT");

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role("STUDENT")
                .id(student.getId())
                .name(student.getName())
                .phone(student.getPhone())
                .isVerified(student.getIsVerified())
                .isPhoneVerified(student.getIsPhoneVerified())
                .build();
    }

    @Transactional
    public void uploadIdCard(Long studentId, String imageUrl) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        student.setIdCardImageUrl(imageUrl);
        studentRepository.save(student);
    }

    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(student -> Boolean.TRUE.equals(student.getIsPhoneVerified()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<StudentResponse> getPendingStudents() {
        return studentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(student -> Boolean.TRUE.equals(student.getIsPhoneVerified()))
                .filter(student -> !Boolean.TRUE.equals(student.getIsVerified()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void verifyStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        student.setIsVerified(true);
        studentRepository.save(student);
    }

    @Transactional
    public void deactivateStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        student.setIsActive(!student.getIsActive());
        studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (student.getIdCardImageUrl() != null) {
            deleteIdCardFile(student.getIdCardImageUrl());
        }
        studentRepository.delete(student);
    }

    private void deleteIdCardFile(String imageUrl) {
        fileStorageService.deleteFile(imageUrl);
    }

    public Student getStudentByPhone(String phone) {
        return studentRepository.findByPhone(normalizePhone(phone))
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private StudentResponse mapToResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .name(student.getName())
                .phone(student.getPhone())
                .studentId(student.getStudentId())
                .department(student.getDepartment())
                .varsityBatch(student.getVarsityBatch())
                .idCardImageUrl(student.getIdCardImageUrl())
                .isVerified(student.getIsVerified())
                .isActive(student.getIsActive())
                .createdAt(student.getCreatedAt())
                .build();
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("880") && cleaned.length() == 13) {
            return cleaned.substring(2);
        }
        if (cleaned.startsWith("01") && cleaned.length() == 11) {
            return cleaned;
        }
        if (cleaned.length() == 10) {
            return "0" + cleaned;
        }
        return cleaned;
    }
}
