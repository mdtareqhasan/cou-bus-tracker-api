package com.cou.bustracker.controller;

import com.cou.bustracker.dto.request.StudentAcademicDetailsRequest;
import com.cou.bustracker.dto.response.StudentResponse;
import com.cou.bustracker.exception.GlobalExceptionHandler;
import com.cou.bustracker.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code PUT /api/auth/student/academic} — the endpoint the app uses after
 * Google Sign-In, when the student row was created without academic details.
 */
public class StudentAuthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StudentAuthController(new StubStudentService(), null))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updateAcademicDetails_validBody_shouldReturnUpdatedProfile() throws Exception {
        mockMvc.perform(put("/api/auth/student/academic")
                        .principal(new UsernamePasswordAuthenticationToken("student@stud.cou.ac.bd", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"department\":\"গণিত\",\"rollNumber\":\"190345\",\"session\":\"2019-2020\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("গণিত"))
                .andExpect(jsonPath("$.rollNumber").value("190345"))
                .andExpect(jsonPath("$.session").value("2019-2020"));
    }

    @Test
    void updateAcademicDetails_missingDepartment_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/auth/student/academic")
                        .principal(new UsernamePasswordAuthenticationToken("student@stud.cou.ac.bd", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rollNumber\":\"190345\",\"session\":\"2019-2020\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAcademicDetails_invalidRollNumber_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/auth/student/academic")
                        .principal(new UsernamePasswordAuthenticationToken("student@stud.cou.ac.bd", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"department\":\"গণিত\",\"rollNumber\":\"roll 19!\",\"session\":\"2019-2020\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAcademicDetails_invalidSession_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/auth/student/academic")
                        .principal(new UsernamePasswordAuthenticationToken("student@stud.cou.ac.bd", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"department\":\"গণিত\",\"rollNumber\":\"190345\",\"session\":\"২০১৯\"}"))
                .andExpect(status().isBadRequest());
    }

    /** Echoes the submitted details back, exactly as the real service does. */
    static class StubStudentService extends StudentService {
        StubStudentService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public StudentResponse updateAcademicDetails(String principal, StudentAcademicDetailsRequest request) {
            return StudentResponse.builder()
                    .id(1L)
                    .name("Student")
                    .email(principal)
                    .department(request.getDepartment())
                    .rollNumber(request.getRollNumber())
                    .session(request.getSession())
                    .isVerified(false)
                    .build();
        }
    }
}
