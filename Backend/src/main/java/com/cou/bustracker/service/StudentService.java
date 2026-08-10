package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.StudentRegisterRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.dto.response.StudentResponse;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    private static final String EDU_MAIL_DOMAIN = "cou.ac.bd";

    @Transactional
    public AuthResponse register(StudentRegisterRequest request) {
        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        if (studentRepository.existsByStudentId(request.getStudentId())) {
            throw new RuntimeException("Student ID already registered");
        }

        boolean isEduMail = request.getEmail().endsWith("@" + EDU_MAIL_DOMAIN);

        Student student = Student.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .studentId(request.getStudentId())
                .department(request.getDepartment())
                .varsityBatch(request.getVarsityBatch())
                .isEduMail(isEduMail)
                .isVerified(false)
                .isActive(true)
                .build();

        studentRepository.save(student);

        String token = jwtService.generateToken(student.getEmail());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .adminName(student.getName())
                .build();
    }

    public AuthResponse login(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        String token = jwtService.generateToken(student.getEmail());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .adminName(student.getName())
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
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<StudentResponse> getPendingStudents() {
        return studentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(s -> !s.getIsVerified())
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
        studentRepository.delete(student);
    }

    public Student getStudentByEmail(String email) {
        return studentRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private StudentResponse mapToResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .studentId(student.getStudentId())
                .department(student.getDepartment())
                .varsityBatch(student.getVarsityBatch())
                .idCardImageUrl(student.getIdCardImageUrl())
                .isEduMail(student.getIsEduMail())
                .isVerified(student.getIsVerified())
                .isActive(student.getIsActive())
                .createdAt(student.getCreatedAt())
                .build();
    }
}
