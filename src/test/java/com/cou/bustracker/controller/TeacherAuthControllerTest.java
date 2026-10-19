package com.cou.bustracker.controller;

import com.cou.bustracker.dto.request.TeacherAcademicDetailsRequest;
import com.cou.bustracker.dto.response.TeacherResponse;
import com.cou.bustracker.exception.GlobalExceptionHandler;
import com.cou.bustracker.service.TeacherService;
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
 * {@code PUT /api/auth/teacher/academic} — the endpoint the app uses after
 * Google Sign-In, when the teacher row was created without those details.
 */
public class TeacherAuthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TeacherAuthController(new StubTeacherService(), null))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updateAcademicDetails_validBody_shouldReturnUpdatedProfile() throws Exception {
        mockMvc.perform(put("/api/auth/teacher/academic")
                        .principal(new UsernamePasswordAuthenticationToken("teacher@cou.ac.bd", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":3,\"employeeId\":\"T-1024\",\"designation\":\"অধ্যাপক\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(3))
                .andExpect(jsonPath("$.teacherId").value("T-1024"))
                .andExpect(jsonPath("$.designation").value("অধ্যাপক"));
    }

    @Test
    void updateAcademicDetails_missingDesignation_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/auth/teacher/academic")
                        .principal(new UsernamePasswordAuthenticationToken("teacher@cou.ac.bd", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":3,\"employeeId\":\"T-1024\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAcademicDetails_missingEmployeeId_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/auth/teacher/academic")
                        .principal(new UsernamePasswordAuthenticationToken("teacher@cou.ac.bd", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":3,\"designation\":\"অধ্যাপক\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAcademicDetails_invalidEmployeeId_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/auth/teacher/academic")
                        .principal(new UsernamePasswordAuthenticationToken("teacher@cou.ac.bd", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":3,\"employeeId\":\"T 10!\",\"designation\":\"অধ্যাপক\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAcademicDetails_missingDepartment_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/auth/teacher/academic")
                        .principal(new UsernamePasswordAuthenticationToken("teacher@cou.ac.bd", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":\"T-1024\",\"designation\":\"অধ্যাপক\"}"))
                .andExpect(status().isBadRequest());
    }

    /** Echoes the submitted details back, exactly as the real service does. */
    static class StubTeacherService extends TeacherService {
        StubTeacherService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public TeacherResponse updateAcademicDetails(String principal, TeacherAcademicDetailsRequest request) {
            return TeacherResponse.builder()
                    .id(1L)
                    .name("Teacher")
                    .email(principal)
                    .departmentId(request.getDepartmentId())
                    .teacherId(request.getEmployeeId())
                    .designation(request.getDesignation())
                    .isVerified(false)
                    .build();
        }
    }
}
