package com.cou.bustracker.service;

import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.repository.TeacherRepository;
import com.cou.bustracker.security.JwtService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code POST /api/auth/google/login} with {@code role=TEACHER} must never
 * accept a student address ({@code *@stud.cou.ac.bd}).
 */
public class TeacherServiceGoogleLoginTest {

    private TeacherService serviceFor(String googleEmail) {
        GoogleTokenService googleTokenService = new GoogleTokenService("test-client-id") {
            @Override
            public GoogleIdentity verify(String idToken) {
                return new GoogleIdentity("google-subject", googleEmail, "Test User");
            }
        };
        TeacherRepository teacherRepository = (TeacherRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{TeacherRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByEmail" -> Optional.empty();
                    case "save" -> args[0];
                    default -> null;
                });
        JwtService jwtService = new JwtService() {
            @Override
            public String generateToken(String subject, String role) {
                return "jwt-token";
            }
        };
        return new TeacherService(teacherRepository, jwtService, null, googleTokenService, null);
    }

    @Test
    void studentDomainEmail_isRejected() {
        TeacherService service = serviceFor("someone@stud.cou.ac.bd");

        ApiException exception = assertThrows(
                ApiException.class, () -> service.loginWithGoogle("google-token"));
        assertTrue(exception.getMessage().contains("name@cou.ac.bd"));
    }

    @Test
    void facultyDomainEmail_signsInAsTeacher() {
        TeacherService service = serviceFor("teacher@cou.ac.bd");

        AuthResponse response = service.loginWithGoogle("google-token");

        assertEquals("TEACHER", response.getRole());
        assertEquals("teacher@cou.ac.bd", response.getEmail());
        assertEquals("jwt-token", response.getAccessToken());
    }
}
