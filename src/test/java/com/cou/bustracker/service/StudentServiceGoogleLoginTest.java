package com.cou.bustracker.service;

import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.repository.TeacherRepository;
import com.cou.bustracker.security.JwtService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@code POST /api/auth/google/login} with {@code role=STUDENT} must never
 * accept an email that is already registered as a teacher — one account, one
 * role. The teacher flow mirrors this rule.
 */
public class StudentServiceGoogleLoginTest {

    private StudentService serviceFor(String googleEmail, boolean emailRegisteredAsTeacher) {
        GoogleTokenService googleTokenService = new GoogleTokenService("test-client-id") {
            @Override
            public GoogleIdentity verify(String idToken) {
                return new GoogleIdentity("google-subject", googleEmail, "Test User");
            }
        };
        StudentRepository studentRepository = (StudentRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{StudentRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByEmail" -> Optional.empty();
                    case "save" -> args[0];
                    default -> null;
                });
        TeacherRepository teacherRepository = (TeacherRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{TeacherRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByEmail" -> emailRegisteredAsTeacher;
                    default -> null;
                });
        JwtService jwtService = new JwtService() {
            @Override
            public String generateToken(String subject, String role) {
                return "jwt-token";
            }
        };
        return new StudentService(studentRepository, teacherRepository, jwtService, null, googleTokenService, null);
    }

    @Test
    void emailAlreadyRegisteredAsTeacher_isRejected() {
        StudentService service = serviceFor("teacher@cou.ac.bd", true);

        ApiException exception = assertThrows(
                ApiException.class, () -> service.loginWithGoogle("google-token"));
        assertEquals(ErrorCode.EMAIL_REGISTERED_AS_TEACHER, exception.getErrorCode());
    }

    @Test
    void studentDomainEmail_signsInAsStudent() {
        StudentService service = serviceFor("student@stud.cou.ac.bd", false);

        AuthResponse response = service.loginWithGoogle("google-token");

        assertEquals("STUDENT", response.getRole());
        assertEquals("student@stud.cou.ac.bd", response.getEmail());
        assertEquals("jwt-token", response.getAccessToken());
    }
}
