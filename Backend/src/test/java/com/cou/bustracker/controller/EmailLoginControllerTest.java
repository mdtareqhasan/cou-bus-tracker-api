package com.cou.bustracker.controller;

import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.exception.GlobalExceptionHandler;
import com.cou.bustracker.service.StudentService;
import com.cou.bustracker.service.TeacherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EmailLoginControllerTest {

    private MockMvc mockMvc;
    private StubStudentService studentService;
    private StubTeacherService teacherService;

    @BeforeEach
    void setUp() {
        studentService = new StubStudentService();
        teacherService = new StubTeacherService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EmailLoginController(studentService, teacherService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_student_shouldReturnAuthResponse() throws Exception {
        mockMvc.perform(post("/api/auth/email-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"STUDENT\",\"email\":\"student@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.accessToken").value("student-token"));
    }

    @Test
    void login_teacher_shouldReturnAuthResponse() throws Exception {
        mockMvc.perform(post("/api/auth/email-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TEACHER\",\"email\":\"teacher@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.email").value("teacher@example.com"))
                .andExpect(jsonPath("$.accessToken").value("teacher-token"));
    }

    @Test
    void login_employee_shouldUseTeacherService() throws Exception {
        mockMvc.perform(post("/api/auth/email-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"EMPLOYEE\",\"email\":\"employee@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.email").value("employee@example.com"));
    }

    @Test
    void login_missingEmail_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/email-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"STUDENT\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_invalidRole_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/email-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\",\"email\":\"admin@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest());
    }

    static class StubStudentService extends StudentService {
        StubStudentService() {
            super(null, null, null, null);
        }

        @Override
        public AuthResponse loginWithEmail(String email, String password) {
            return AuthResponse.builder()
                    .accessToken("student-token")
                    .tokenType("Bearer")
                    .role("STUDENT")
                    .email(email)
                    .name("Student")
                    .isVerified(true)
                    .build();
        }
    }

    static class StubTeacherService extends TeacherService {
        StubTeacherService() {
            super(null, null, null, null);
        }

        @Override
        public AuthResponse loginWithEmail(String email, String password) {
            return AuthResponse.builder()
                    .accessToken("teacher-token")
                    .tokenType("Bearer")
                    .role("TEACHER")
                    .email(email)
                    .name("Teacher")
                    .isVerified(true)
                    .build();
        }
    }
}
