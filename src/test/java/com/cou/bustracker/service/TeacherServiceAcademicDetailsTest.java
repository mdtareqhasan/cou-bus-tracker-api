package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.TeacherAcademicDetailsRequest;
import com.cou.bustracker.dto.response.TeacherResponse;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A Google-created teacher row has no department/employee ID/designation. It
 * must be possible to fill them exactly once — and never to swap them
 * afterwards, because an employee ID may only ever belong to one account.
 */
public class TeacherServiceAcademicDetailsTest {

    private TeacherRepository teacherRepository;
    private TeacherService teacherService;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        teacherRepository = mock(TeacherRepository.class);
        teacherService = new TeacherService(teacherRepository, null, null, null, null, null);
        teacher = Teacher.builder()
                .id(11L)
                .name("Teacher")
                .email("teacher@cou.ac.bd")
                .isVerified(false)
                .isActive(true)
                .build();
        when(teacherRepository.findByPhoneOrEmail("teacher@cou.ac.bd"))
                .thenReturn(Optional.of(teacher));
        when(teacherRepository.save(any(Teacher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private TeacherAcademicDetailsRequest request(String department, String employeeId, String designation) {
        return new TeacherAcademicDetailsRequest(department, employeeId, designation);
    }

    @Test
    void googleCreatedTeacher_canFillDetailsOnce() {
        TeacherResponse response = teacherService.updateAcademicDetails(
                "teacher@cou.ac.bd",
                request("গণিত", "T-1024", "অধ্যাপক"));

        assertEquals("গণিত", response.getDepartment());
        assertEquals("T-1024", response.getTeacherId());
        assertEquals("অধ্যাপক", response.getDesignation());
        assertEquals("T-1024", teacher.getTeacherId());
        verify(teacherRepository).save(teacher);
    }

    @Test
    void employeeIdOwnedByAnotherAccount_isRejected() {
        when(teacherRepository.existsByTeacherId("T-1024")).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> teacherService.updateAcademicDetails(
                        "teacher@cou.ac.bd", request("গণিত", "T-1024", "অধ্যাপক")));

        assertEquals(ErrorCode.TEACHER_ID_ALREADY_REGISTERED, exception.getErrorCode());
        verify(teacherRepository, never()).save(any(Teacher.class));
        assertNull(teacher.getTeacherId());
    }

    @Test
    void populatedEmployeeId_cannotBeChanged() {
        teacher.setDepartment("গণিত");
        teacher.setTeacherId("T-1024");
        teacher.setDesignation("অধ্যাপক");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> teacherService.updateAcademicDetails(
                        "teacher@cou.ac.bd", request("গণিত", "T-9999", "অধ্যাপক")));

        assertEquals(ErrorCode.EMPLOYEE_ID_IMMUTABLE, exception.getErrorCode());
        assertEquals("T-1024", teacher.getTeacherId());
        verify(teacherRepository, never()).save(any(Teacher.class));
    }

    @Test
    void populatedDesignation_cannotBeChanged() {
        teacher.setDepartment("গণিত");
        teacher.setTeacherId("T-1024");
        teacher.setDesignation("অধ্যাপক");

        assertThrows(ApiException.class,
                () -> teacherService.updateAcademicDetails(
                        "teacher@cou.ac.bd", request("গণিত", "T-1024", "সহযোগী অধ্যাপক")));
        assertEquals("অধ্যাপক", teacher.getDesignation());
    }

    @Test
    void identicalDetails_forAlreadyCompleteProfile_areAcceptedWithoutWrite() {
        teacher.setDepartment("গণিত");
        teacher.setTeacherId("T-1024");
        teacher.setDesignation("অধ্যাপক");

        TeacherResponse response = teacherService.updateAcademicDetails(
                "teacher@cou.ac.bd",
                request("গণিত", "T-1024", "অধ্যাপক"));

        assertEquals("T-1024", response.getTeacherId());
        verify(teacherRepository, never()).save(any(Teacher.class));
    }

    @Test
    void missingValue_isRejected() {
        assertThrows(ApiException.class,
                () -> teacherService.updateAcademicDetails(
                        "teacher@cou.ac.bd", request(null, "T-1024", "অধ্যাপক")));

        assertThrows(ApiException.class,
                () -> teacherService.updateAcademicDetails(
                        "teacher@cou.ac.bd", request("গণিত", " ", "অধ্যাপক")));

        assertThrows(ApiException.class,
                () -> teacherService.updateAcademicDetails(
                        "teacher@cou.ac.bd", request("গণিত", "T-1024", null)));
    }
}
