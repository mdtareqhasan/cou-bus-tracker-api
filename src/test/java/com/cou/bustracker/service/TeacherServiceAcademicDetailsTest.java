package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.TeacherAcademicDetailsRequest;
import com.cou.bustracker.dto.response.TeacherResponse;
import com.cou.bustracker.entity.AcademicUnit;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.repository.AcademicUnitRepository;
import com.cou.bustracker.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A Google-created teacher row has no department/employee ID/designation. It
 * must be possible to fill them exactly once — and never to swap them
 * afterwards, because an employee ID may only ever belong to one account.
 *
 * <p>The department is now referenced by row id and may point at either a
 * DEPARTMENT or an OFFICE unit — a teacher works in a teaching department or
 * an administrative office.
 */
public class TeacherServiceAcademicDetailsTest {

    private static final Long DEPT_ID = 3L;
    private static final Long OFFICE_ID = 30L;

    private TeacherRepository teacherRepository;
    private AcademicUnitRepository academicUnitRepository;
    private TeacherService teacherService;
    private Teacher teacher;
    private AcademicUnit mathematics;

    @BeforeEach
    void setUp() {
        teacherRepository = mock(TeacherRepository.class);
        academicUnitRepository = mock(AcademicUnitRepository.class);
        teacherService = new TeacherService(teacherRepository, null, academicUnitRepository,
                null, null, null, null);

        mathematics = AcademicUnit.builder()
                .id(DEPT_ID)
                .unitType(AcademicUnit.UnitType.DEPARTMENT)
                .nameBn("গণিত")
                .nameEn("Mathematics")
                .isActive(true)
                .build();
        when(academicUnitRepository.findById(DEPT_ID)).thenReturn(Optional.of(mathematics));

        teacher = Teacher.builder()
                .id(11L)
                .name("Teacher")
                .email("teacher@cou.ac.bd")
                .isVerified(false)
                .isActive(true)
                .build();
        when(teacherRepository.findByEmail("teacher@cou.ac.bd"))
                .thenReturn(Optional.of(teacher));
        when(teacherRepository.save(any(Teacher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private TeacherAcademicDetailsRequest request(Long departmentId, String employeeId, String designation) {
        return new TeacherAcademicDetailsRequest(departmentId, employeeId, designation);
    }

    @Test
    void googleCreatedTeacher_canFillDetailsOnce() {
        TeacherResponse response = teacherService.updateAcademicDetails(
                "teacher@cou.ac.bd",
                request(DEPT_ID, "T-1024", "অধ্যাপক"));

        assertEquals(DEPT_ID, response.getDepartmentId());
        assertEquals("গণিত", response.getDepartment());
        assertEquals("T-1024", response.getTeacherId());
        assertEquals("অধ্যাপক", response.getDesignation());
        assertEquals("T-1024", teacher.getTeacherId());
        verify(teacherRepository).save(teacher);
    }

    @Test
    void officeUnit_isAllowedForTeacher() {
        AcademicUnit registrarOffice = AcademicUnit.builder()
                .id(OFFICE_ID)
                .unitType(AcademicUnit.UnitType.OFFICE)
                .nameBn("রেজিস্ট্রারের কার্যালয়")
                .nameEn("Office of the Registrar")
                .isActive(true)
                .build();
        when(academicUnitRepository.findById(OFFICE_ID)).thenReturn(Optional.of(registrarOffice));

        TeacherResponse response = teacherService.updateAcademicDetails(
                "teacher@cou.ac.bd",
                request(OFFICE_ID, "T-1024", "রেজিস্ট্রার"));

        assertEquals(OFFICE_ID, response.getDepartmentId());
        assertEquals("রেজিস্ট্রারের কার্যালয়", response.getDepartment());
        assertEquals(registrarOffice, teacher.getDepartment());
        verify(teacherRepository).save(teacher);
    }

    @Test
    void employeeIdOwnedByAnotherAccount_isRejected() {
        when(teacherRepository.existsByTeacherId("T-1024")).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> teacherService.updateAcademicDetails(
                        "teacher@cou.ac.bd", request(DEPT_ID, "T-1024", "অধ্যাপক")));

        assertEquals(ErrorCode.TEACHER_ID_ALREADY_REGISTERED, exception.getErrorCode());
        verify(teacherRepository, never()).save(any(Teacher.class));
        assertNull(teacher.getTeacherId());
    }

    @Test
    void populatedEmployeeId_cannotBeChanged() {
        teacher.setDepartment(mathematics);
        teacher.setTeacherId("T-1024");
        teacher.setDesignation("অধ্যাপক");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> teacherService.updateAcademicDetails(
                        "teacher@cou.ac.bd", request(DEPT_ID, "T-9999", "অধ্যাপক")));

        assertEquals(ErrorCode.EMPLOYEE_ID_IMMUTABLE, exception.getErrorCode());
        assertEquals("T-1024", teacher.getTeacherId());
        verify(teacherRepository, never()).save(any(Teacher.class));
    }

    @Test
    void populatedDesignation_cannotBeChanged() {
        teacher.setDepartment(mathematics);
        teacher.setTeacherId("T-1024");
        teacher.setDesignation("অধ্যাপক");

        assertThrows(ApiException.class,
                () -> teacherService.updateAcademicDetails(
                        "teacher@cou.ac.bd", request(DEPT_ID, "T-1024", "সহযোগী অধ্যাপক")));
        assertEquals("অধ্যাপক", teacher.getDesignation());
    }

    @Test
    void populatedDepartment_cannotBeSwapped() {
        teacher.setDepartment(mathematics);
        teacher.setTeacherId("T-1024");
        teacher.setDesignation("অধ্যাপক");

        AcademicUnit physics = AcademicUnit.builder()
                .id(4L)
                .unitType(AcademicUnit.UnitType.DEPARTMENT)
                .nameBn("পদার্থবিজ্ঞান")
                .isActive(true)
                .build();
        when(academicUnitRepository.findById(4L)).thenReturn(Optional.of(physics));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> teacherService.updateAcademicDetails(
                        "teacher@cou.ac.bd", request(4L, "T-1024", "অধ্যাপক")));

        assertEquals(ErrorCode.DEPARTMENT_IMMUTABLE, exception.getErrorCode());
        assertEquals(mathematics, teacher.getDepartment());
        verify(teacherRepository, never()).save(any(Teacher.class));
    }

    @Test
    void identicalDetails_forAlreadyCompleteProfile_areAcceptedWithoutWrite() {
        teacher.setDepartment(mathematics);
        teacher.setTeacherId("T-1024");
        teacher.setDesignation("অধ্যাপক");

        TeacherResponse response = teacherService.updateAcademicDetails(
                "teacher@cou.ac.bd",
                request(DEPT_ID, "T-1024", "অধ্যাপক"));

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
                        "teacher@cou.ac.bd", request(DEPT_ID, " ", "অধ্যাপক")));

        assertThrows(ApiException.class,
                () -> teacherService.updateAcademicDetails(
                        "teacher@cou.ac.bd", request(DEPT_ID, "T-1024", null)));
    }
}
