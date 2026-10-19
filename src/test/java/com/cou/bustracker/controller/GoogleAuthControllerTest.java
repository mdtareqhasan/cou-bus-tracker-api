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

public class GoogleAuthControllerTest {

    private MockMvc mockMvc;
    private StubStudentService studentService;
    private StubTeacherService teacherService;

    @BeforeEach
    void setUp() {
        studentService = new StubStudentService();
        teacherService = new StubTeacherService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GoogleAuthController(studentService, teacherService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_student_shouldReturnAuthResponse() throws Exception {
        mockMvc.perform(post("/api/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"google-token\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.email").value("student@stud.cou.ac.bd"))
                .andExpect(jsonPath("$.accessToken").value("student-token"));
    }

    @Test
    void login_teacher_shouldReturnAuthResponse() throws Exception {
        mockMvc.perform(post("/api/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"google-token\",\"role\":\"TEACHER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.email").value("teacher@cou.ac.bd"))
                .andExpect(jsonPath("$.accessToken").value("teacher-token"));
    }

    @Test
    void login_missingIdToken_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"STUDENT\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_missingRole_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"google-token\"}"))
                .andExpect(status().isBadRequest());
    }

    static class StubStudentService extends StudentService {
        StubStudentService() {
            super(null, null, null, null, null, null, null, null);
        }

        @Override
        public AuthResponse loginWithGoogle(String idToken) {
            return AuthResponse.builder()
                    .accessToken("student-token")
                    .tokenType("Bearer")
                    .role("STUDENT")
                    .email("student@stud.cou.ac.bd")
                    .name("Student")
                    .isVerified(false)
                    .build();
        }
    }

    static class StubTeacherService extends TeacherService {
        StubTeacherService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public AuthResponse loginWithGoogle(String idToken) {
            return AuthResponse.builder()
                    .accessToken("teacher-token")
                    .tokenType("Bearer")
                    .role("TEACHER")
                    .email("teacher@cou.ac.bd")
                    .name("Teacher")
                    .isVerified(false)
                    .build();
        }
    }
}
