package com.cou.bustracker.security;

import com.cou.bustracker.entity.Student;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.repository.TeacherRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The 403 gate: a student without department/roll/session — or a teacher
 * without department/employee ID/designation — may not use anything outside
 * the auth flow, and must be able to complete the profile through it.
 */
public class ProfileCompletionInterceptorTest {

    private StudentRepository studentRepository;
    private TeacherRepository teacherRepository;
    private ProfileCompletionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        teacherRepository = mock(TeacherRepository.class);
        interceptor = new ProfileCompletionInterceptor(studentRepository, teacherRepository, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String principal, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        return request;
    }

    @Test
    void incompleteStudent_onProtectedPath_isBlocked() throws Exception {
        authenticate("student@stud.cou.ac.bd", "STUDENT");
        when(studentRepository.findByPhoneOrEmail("student@stud.cou.ac.bd"))
                .thenReturn(Optional.of(Student.builder()
                        .email("student@stud.cou.ac.bd")
                        .department(null)
                        .rollNumber(null)
                        .session(null)
                        .build()));
        MockHttpServletRequest request = request("/api/notifications/fcm-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, null));

        assertEquals(403, response.getStatus());
        String body = response.getContentAsString();
        assertTrue(body.contains(ProfileCompletionInterceptor.ERROR_CODE));
        assertTrue(body.contains("roll number"));
    }

    @Test
    void incompleteStudent_onAuthPath_isAllowed() throws Exception {
        authenticate("student@stud.cou.ac.bd", "STUDENT");
        when(studentRepository.findByPhoneOrEmail("student@stud.cou.ac.bd"))
                .thenReturn(Optional.of(Student.builder().email("student@stud.cou.ac.bd").build()));
        MockHttpServletRequest request = request("/api/auth/student/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, null));
        assertEquals(200, response.getStatus());
    }

    @Test
    void completeStudent_isAllowed() throws Exception {
        authenticate("student@stud.cou.ac.bd", "STUDENT");
        when(studentRepository.findByPhoneOrEmail("student@stud.cou.ac.bd"))
                .thenReturn(Optional.of(Student.builder()
                        .email("student@stud.cou.ac.bd")
                        .department("গণিত")
                        .rollNumber("190345")
                        .session("2019-2020")
                        .build()));
        MockHttpServletRequest request = request("/api/notifications/fcm-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, null));
        assertEquals(200, response.getStatus());
    }

    @Test
    void incompleteTeacher_onProtectedPath_isBlocked() throws Exception {
        authenticate("teacher@cou.ac.bd", "TEACHER");
        when(teacherRepository.findByPhoneOrEmail("teacher@cou.ac.bd"))
                .thenReturn(Optional.of(Teacher.builder()
                        .email("teacher@cou.ac.bd")
                        .department("গণিত")
                        .teacherId(null)
                        .designation(null)
                        .build()));
        MockHttpServletRequest request = request("/api/notifications/fcm-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, null));

        assertEquals(403, response.getStatus());
        String body = response.getContentAsString();
        assertTrue(body.contains(ProfileCompletionInterceptor.ERROR_CODE));
        assertTrue(body.contains("employee ID"));
    }

    @Test
    void teacher_onAuthPath_isAllowed() throws Exception {
        authenticate("teacher@cou.ac.bd", "TEACHER");
        MockHttpServletRequest request = request("/api/auth/teacher/academic");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, null));
    }

    @Test
    void unauthenticatedRequest_isAllowed() throws Exception {
        MockHttpServletRequest request = request("/api/buses");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, null));
    }

    @Test
    void admin_isAllowed() throws Exception {
        authenticate("admin@coubustracker.com", "ADMIN");
        MockHttpServletRequest request = request("/api/admin/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, null));
    }

    @Test
    void corsPreflight_isAllowed() throws Exception {
        authenticate("student@stud.cou.ac.bd", "STUDENT");
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/notifications/fcm-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, null));
    }
}
